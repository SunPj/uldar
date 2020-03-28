package com.github.sunpj.uldar.extension

import javax.inject.Inject
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}
import scala.util.control.NonFatal

/**
  * Registry of application ApiExtension's
  *
  * @param extensions set of ApiExtension implementations
  */
case class ApiExtensionRegistry[I](extensions: Set[ApiExtension[I]]) {

  private val extensionsMap = extensions.map(e => (e.name, e)).toMap

  /**
    * Returns extension by name
    *
    * @param name extension name
    * @return some ApiExtension by given name, empty if no ApiExtension registered for given name
    */
  def getExtension(name: String): Option[ApiExtension[I]] = extensionsMap.get(name)
}

/**
  * Manages all extension API calls
  *
  * @param extensionRegistry registry of extensions
  * @param ec                scala execution context
  */
class ExtensionService[I] @Inject()(extensionRegistry: ApiExtensionRegistry[I])(implicit ec: ExecutionContext) extends WithLogger {

  /**
    * Delegates API call to specific extension
    *
    * @param path    url path request is sent to
    * @param request API call request
    * @return ApiCallResponse
    */
  def processApiCall(path: Seq[String], request: ApiCallRequest[I, JsValue]): Future[ApiCallResponse] = {
    path.headOption.flatMap(extensionRegistry.getExtension) match {
      case Some(extension) if extension.apiCallHandler.isDefinedAt(path.tail) =>
        Try {
          extension.apiCallHandler(path.tail)(request).recover {
            case NonFatal(e) =>
              logger.error(systemErrorMessage(path, request), e)
              SystemError
          }
        } match {
          case scala.util.Success(response) =>
            response
          case Failure(e) =>
            logger.error(systemErrorMessage(path, request), e)
            Future.successful(SystemError)
        }
      case None =>
        logger.warn(s"Extension not found for path $path")
        Future.successful(UnsupportedRequest)
    }
  }

  private def systemErrorMessage(path: Seq[String], request: ApiCallRequest[I, JsValue]) = {
    s"System error on processing request = ${Json.stringify(request.data)} by for path $path"
  }
}