package com.github.sunpj.uldar

import javax.inject.Inject
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Api extension
  *
  * Write your own implementations to extend application backend API
  */
trait ApiExtension {
  /**
    * Name of extension
    *
    * @return
    */
  def name: String

  /**
    * Processes API calls
    *
    * @param request api request
    * @return
    */
  def processApiCall(request: ApiCallRequest): Future[JsValue]
}

/**
  * Registry of application ApiExtension's
  *
  * @param extensions set of ApiExtension implementations
  */
case class ApiExtensionRegistry(extensions: Set[ApiExtension]) {

  private val extensionsMap = extensions.map(e => (e.name, e)).toMap

  /**
    * Returns extension by name
    *
    * @param name extension name
    * @return some ApiExtension by given name, empty if no ApiExtension registered for given name
    */
  def getExtension(name: String): Option[ApiExtension] = extensionsMap.get(name)
}

/**
  * Manages all extension API calls
  *
  * @param extensionRegistry registry of extensions
  * @param ec                scala execution context
  */
class ExtensionService @Inject()(extensionRegistry: ApiExtensionRegistry)(implicit ec: ExecutionContext) {

  /**
    * Delegates API call to specific extension
    *
    * @param extensionName extension name request is sent to
    * @param request       API call request
    * @return ApiCallResponse
    */
  def processApiCall(extensionName: String, request: ApiCallRequest): Future[ApiCallResponse] = {
    extensionRegistry.getExtension(extensionName) match {
      case Some(extension) =>
        extension.processApiCall(request).map(ApiCallResponse)
      case None =>
        Future.successful(
          ApiCallResponse(
            Json.obj("error" -> s"Extension $extensionName not found")
          )
        )
    }
  }
}

case class ApiCallRequest(data: JsValue)

case class ApiCallResponse(data: JsValue)
