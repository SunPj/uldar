package com.github.sunpj.uldar

import javax.inject.Inject
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Serves as backend API provider for UI widget specified by `id`
  */
trait WidgetDataProvider {
  /**
    * Identity of widget which this WidgetDataProvider provides data for
    *
    * @return
    */
  def id: String

  /**
    * Provides render model for widget
    *
    * @param configuration widget configuration
    * @return render model for widget
    */
  def getRenderModel(configuration: JsValue): Future[JsValue]

  /**
    * Process and returns the response for given API request sent by widget
    *
    * @param request widget api request
    * @return render model for widget
    */
  def processApiRequest(request: WidgetApiRequest): Future[JsValue]
}

/**
  * WidgetDataProvider for static data which means widget has no logic and sends back configuration json as
  * render model.
  *
  * The aim of this provider is to facilitate of creating static widgets on frontend side, so StaticWidgetDataProvider
  * can be used as a widget model storage
  *
  * @param id identity of widget which this WidgetDataProvider provides data for
  */
abstract class StaticWidgetDataProvider(val id: String) extends WidgetDataProvider {
  /**
    * Provides render model for widget
    *
    * @param configuration widget configuration
    * @return render model for widget
    */
  def getRenderModel(configuration: JsValue): Future[JsValue] = Future.successful(configuration)

  /**
    * Process and returns the response for given API request sent by widget
    *
    * @param request widget api request
    * @return render model for widget
    */
  def processApiRequest(request: WidgetApiRequest): Future[JsValue] = {
    Future.failed(new UnsupportedOperationException(s"API calls are not supported for static widget"))
  }
}

/**
  * Registry of all known [[WidgetDataProvider]]s
  *
  * @param widgetDataProviders widget data providers
  */
case class WidgetDataProviderRegistry(widgetDataProviders: Seq[WidgetDataProvider]) {
  /**
    * Map (id -> Widget)
    */
  private val widgetDataProvidersMap: Map[String, WidgetDataProvider] = {
    widgetDataProviders.map(w => (w.id, w)).toMap
  }

  /**
    * Returns [[WidgetDataProvider]] for given widget id
    * @param id widget id
    * @return maybe a [[WidgetDataProvider]] for given widget id if there is any, otherwise none
    */
  def getWidgetDataProvider(id: String): Option[WidgetDataProvider] = widgetDataProvidersMap.get(id)

  /**
    * Checks whether this registry has WidgetDataProvider for given widget id
    *
    * @param id widget id
    * @return true if there is WidgetDataProvider for given widget id in this registry, otherwise false
    */
  def hasWidgetDataProvider(id: String): Boolean = getWidgetDataProvider(id).isDefined
}

/**
  * @param registry registry of WidgetDataProvider
  * @param ec scala execution context to deal with scala Futures
  */
class WidgetDataProviderService @Inject()(registry: WidgetDataProviderRegistry)(implicit ec: ExecutionContext) {

  /**
    * Returns the render data model for given [[WidgetRenderingConfiguration]]
    *
    * @param configuration widget rendering configuration [[WidgetRenderingConfiguration]]
    * @return data model to render the widget specified by [[WidgetRenderingConfiguration]]
    */
  def getWidgetRenderModel(configuration: WidgetRenderingConfiguration): Future[JsValue] = {
    registry.getWidgetDataProvider(configuration.id) match {
      case Some(wdp) =>
        wdp.getRenderModel(configuration.configuration).flatMap { renderModel =>
          val nestedModelsFutures = configuration.nested.map(getWidgetRenderModel)
          Future.sequence(nestedModelsFutures).map { nestedModels =>
            Json.obj(
              "widgetId" -> configuration.id,
              "model" -> renderModel,
              "nested" -> nestedModels
            )
          }
        }
      case None =>
        Future.successful(Json.obj("error" -> "No data provider found for widget"))
    }
  }

  /**
    * Process and returns the response for the API request sent by widget with given id
    *
    * @param id            identity of a widget which initiated the request
    * @param request       widget api request
    * @return maybe the response for the API request sent by widget with given id,
    *         empty case means no widget data provider was found
    */
  def processApiRequest(id: String, request: WidgetApiRequest): Future[Option[JsValue]] = {
    registry.getWidgetDataProvider(id) match {
      case Some(wdp) =>
        wdp.processApiRequest(request).map(Some(_))
      case None =>
        Future.successful(None)
    }
  }
}

/**
  * All Widget api request
  *
  * @param data api request in json
  */
case class WidgetApiRequest(data: JsValue)
