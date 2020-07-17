package amf.plugins.document.webapi.parser.spec.declaration

import amf.core.parser._
import amf.core.validation.SeverityLevels
import amf.plugins.document.webapi.contexts.parser.OasLikeWebApiContext
import amf.plugins.document.webapi.contexts.{CustomClosedShapeContextDecorator, SpecField, SpecNode}
import amf.plugins.domain.webapi.models.security.SecurityScheme
import org.yaml.model.{YMap, YPart}

object OasSecuritySchemeParser {
  def apply(part: YPart, adopt: SecurityScheme => SecurityScheme)(implicit ctx: OasLikeWebApiContext) =
    new OasSecuritySchemeParser(part, adopt)(new CustomClosedShapeContextDecorator(ctx, Oas2SecuritySchemeNodes))

  val flowPossibleFields = Set(
    "type",
    "description",
    "flow",
    "scopes"
  )

  val Oas2SecuritySchemeNodes = Map(
    "basic" -> SpecNode(
      requiredFields = Set(SpecField("type", SeverityLevels.VIOLATION)), //should delete?
      possibleFields = Set("description")
    ),
    "oauth2" -> SpecNode(
      requiredFields = Set(
        SpecField("flow", SeverityLevels.VIOLATION),
        SpecField("scopes", SeverityLevels.VIOLATION)
      ),
      possibleFields = Set(
        "type",
        "description",
        "authorizationUrl",
        "tokenUrl",
        "flow",
        "scopes",
      )
    ),
    "implicit" -> SpecNode(
      requiredFields = Set(SpecField("authorizationUrl", SeverityLevels.WARNING)),
      possibleFields = flowPossibleFields
    ),
    "accessCode" -> SpecNode(
      requiredFields = Set(
        SpecField("authorizationUrl", SeverityLevels.WARNING),
        SpecField("tokenUrl", SeverityLevels.WARNING)
      ),
      possibleFields = flowPossibleFields
    ),
    "application" -> SpecNode(
      requiredFields = Set(SpecField("tokenUrl", SeverityLevels.WARNING)),
      possibleFields = flowPossibleFields
    ),
    "password" -> SpecNode(
      requiredFields = Set(SpecField("tokenUrl", SeverityLevels.WARNING)),
      possibleFields = flowPossibleFields
    )
  )
}

case class OasSecuritySchemeParser(part: YPart, adopt: SecurityScheme => SecurityScheme)(
    implicit ctx: OasLikeWebApiContext)
    extends OasLikeSecuritySchemeParser(part, adopt) {
  override def closedShape(scheme: SecurityScheme, map: YMap, shape: String): Unit = {

    val key = map.key("type").map(_.value.as[String]) match {
      case Some("basic")  => "basic"
      case Some("oauth2") => "oauth2"
      case _              => shape
    }
    ctx.closedShape(scheme.id, map, key)

    val flow = map.key("flow").map(_.value.as[String]) match {
      case Some("implicit")    => "implicit"
      case Some("accessCode")  => "accessCode"
      case Some("application") => "application"
      case Some("password")    => "password"
      case _                   => shape
    }
    if (!flow.isEmpty) ctx.closedShape(scheme.id, map, flow)
  }
}
