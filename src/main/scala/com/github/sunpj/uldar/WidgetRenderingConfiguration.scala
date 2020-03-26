package com.github.sunpj.uldar

import javax.inject.Inject
import play.api.libs.json.JsValue

import scala.concurrent.{ExecutionContext, Future}

/**
  * Widget rendering configuration
  *
  * TODO introduce a slot for nesting widgets
  * TODO introduce a version for widget
  *
  * @param id            identity of a widget which supposed to render the content
  * @param configuration configuration of a widget which supposed to render the content
  * @param nested        a list of nested widgets
  * @see [[WidgetDataProvider]]
  */
case class WidgetRenderingConfiguration(id: String, configuration: JsValue, nested: List[WidgetRenderingConfiguration])

/**
  * Set of methods to process content
  *
  * @param registry   widget registry of [[WidgetDataProvider]]s
  * @param repository repository to store WidgetRenderingConfiguration instances
  * @tparam T identity of stored [[WidgetRenderingConfiguration]] entities
  * @param ec scala execution context
  */
class WidgetRenderingConfigurationService[T] @Inject()(registry: WidgetDataProviderRegistry, repository: WidgetRenderingRepository[T])(implicit ec: ExecutionContext) {

  /**
    * Saves [[WidgetRenderingConfiguration]] in repository checking existence of [[WidgetDataProvider]]s for
    * each widget rendering configuration recursively
    *
    * @param wrc widget rendering configuration
    * @throws WidgetDataProviderNotFoundException if there is no WidgetDataProvider associated with widget id
    * @return widget rendering configuration id
    */
  def create(wrc: WidgetRenderingConfiguration): Future[T] = {
    getNonRegisteredWidgetIds(wrc) match {
      case Nil => repository.save(wrc)
      case ids => throw WidgetDataProviderNotFoundException(ids)
    }
  }

  /**
    * Recursively collects all widget ids for given [[WidgetRenderingConfiguration]]
    *
    * @param wrc widget rendering configuration [[WidgetRenderingConfiguration]]
    * @return all widget ids for given [[WidgetRenderingConfiguration]]
    */
  private def getWidgetIds(wrc: WidgetRenderingConfiguration): List[String] = {
    wrc.id :: wrc.nested.flatMap(getWidgetIds)
  }

  /**
    * Returns the list of widget id's for which there is no registered data providers
    *
    * @param wrc widget rendering configuration
    * @return the list of widget id's for which there is no registered data providers
    */
  private def getNonRegisteredWidgetIds(wrc: WidgetRenderingConfiguration) = {
    val widgetIds = getWidgetIds(wrc)
    widgetIds.filterNot(registry.hasWidgetDataProvider)
  }

  /**
    * Deletes [[WidgetRenderingConfiguration]] by id
    *
    * @param id WidgetRenderingConfiguration id
    * @return whether WidgetRenderingConfiguration was deleted or not,
    *         false means that no element is found by given ID in repository
    */
  def delete(id: T): Future[Boolean] = repository.delete(id)

  /**
    * Updates [[WidgetRenderingConfiguration]] by id
    *
    * @param id  WidgetRenderingConfiguration id
    * @param wrc WidgetRenderingConfiguration
    * @throws WidgetDataProviderNotFoundException if there is no WidgetDataProvider associated with widget id
    * @return whether WidgetRenderingConfiguration was updated or not,
    *         false means that no element is found by given ID
    */
  def update(id: T, wrc: WidgetRenderingConfiguration): Future[Boolean] = {
    getNonRegisteredWidgetIds(wrc) match {
      case Nil => repository.update(id, wrc)
      case ids => throw WidgetDataProviderNotFoundException(ids)
    }
  }
}

case class WidgetDataProviderNotFoundException(widgetIds: List[String])
  extends RuntimeException(s"WidgetDataProvider not found for widgets ids = [$widgetIds]")

/**
  * Repository to store [[WidgetRenderingConfiguration]]s
  *
  * @tparam T Type of identity of stored [[WidgetRenderingConfiguration]] entities
  */
trait WidgetRenderingRepository[T] {

  /**
    * Saves content in storage
    *
    * @param wrc WidgetRenderingConfiguration to save
    * @return WidgetRenderingConfiguration identity
    */
  def save(wrc: WidgetRenderingConfiguration): Future[T]

  /**
    * Deletes [[WidgetRenderingConfiguration]] by given identity
    *
    * @param id [[WidgetRenderingConfiguration]] id
    * @return whether content was deleted or not. False means that no content is found by given Id
    */
  def delete(id: T): Future[Boolean]

  /**
    * Updates content by given id
    *
    * @param id  [[WidgetRenderingConfiguration]] id
    * @param wrc WidgetRenderingConfiguration
    * @return whether content was updated or not. False means that no content is found by given ID
    */
  def update(id: T, wrc: WidgetRenderingConfiguration): Future[Boolean]
}
