package com.github.sunpj.uldar.extension

import org.slf4j.LoggerFactory
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json._

import scala.concurrent.Future

/**
  * Api extension
  *
  * Write your own implementations to extend application backend API
  *
  * @tparam I User identity type
  */
trait ApiExtension[I] {
  /**
    * Name of extension
    *
    * @return
    */
  def name: String

  /**
    * API call handler
    *
    * Partial function from Path to ApiCall
    */
  def apiCallHandler: PartialFunction[Seq[String], ApiCall]

  trait ApiCall extends (ApiCallRequest[I, JsValue] => Future[ApiCallResponse])

  object ApiCall extends WithLogger {
    def apply[T](f: ApiCallRequest[I, T] => Future[ApiCallResponse])(implicit r: Reads[T]): ApiCall = { request =>
      r.reads(request.data) match {
        case JsSuccess(value, _) =>
          f(ApiCallRequest(value, request.identity))
        case e: JsError =>
          logger.warn(s"Request can't be converted to action model. ${Json.stringify(request.data)}. Error: ${Json.stringify(JsError.toJson(e))}")
          Future.successful(InvalidRequest(Set("Request can't be parsed")))
      }
    }
  }

}

trait WithLogger {
  protected val logger = LoggerFactory.getLogger(this.getClass.getName)
}

case class ApiCallRequest[I, D](data: D, identity: Option[I])

sealed trait ApiCallResponse
trait Ok extends ApiCallResponse with (() => JsValue)
trait NonOk extends ApiCallResponse
case object Forbidden extends NonOk

case class InvalidRequest(errors: Set[String]) extends NonOk
object InvalidRequest {
  def apply(error: String): InvalidRequest = InvalidRequest(Set(error))
}
case object NotFound extends NonOk
case object UnsupportedRequest extends NonOk
case object SystemError extends NonOk

object Ok {
  def apply: Ok = () => JsNull
  def apply[T](v: T)(implicit w: Writes[T]): Ok = () => w.writes(v)
  def apply(fields: (String, JsValueWrapper)*): Ok = () => Json.obj(fields: _*)
}
