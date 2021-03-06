package amf.plugins.document.webapi.resolution.pipelines.compatibility.raml

import amf.core.errorhandling.ErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.resolution.stages.ResolutionStage
import amf.plugins.domain.webapi.metamodel.RequestModel
import amf.plugins.domain.webapi.models.EndPoint

class PushSingleOperationPathParams()(override implicit val errorHandler: ErrorHandler) extends ResolutionStage {

  def checkUriParams(endpoint: EndPoint): EndPoint = {
    if (endpoint.operations.size == 1 && Option(endpoint.operations.head.request).isDefined) {
      val operation = endpoint.operations.head
      val uriParams = operation.request.uriParameters
      if (uriParams.nonEmpty) {
        operation.request.fields.removeField(RequestModel.UriParameters)
        endpoint.withParameters(uriParams.map { param =>
          param.withRequired(true) // URI parameters are always required
        })
      } else endpoint
    } else endpoint
  }

  override def resolve[T <: BaseUnit](model: T): T = {
    try {
      model.iterator().foreach {
        case endpoint: EndPoint =>
          checkUriParams(endpoint)
        case _ => // ignore
      }
    } catch {
      case _: Throwable => // ignore: we don't want this to break anything
    }
    model
  }
}
