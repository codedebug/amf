package amf.plugins.document.webapi.contexts.parser

import amf.core.client.ParsingOptions
import amf.core.model.document.ExternalFragment
import amf.core.model.domain.Shape
import amf.core.parser.{ParsedReference, ParserContext, YMapOps}
import amf.plugins.document.webapi.JsonSchemaPlugin
import amf.plugins.document.webapi.contexts.parser.raml.RamlWebApiContext
import amf.plugins.document.webapi.contexts.{SpecVersionFactory, WebApiContext}
import amf.plugins.document.webapi.parser.spec.{OasLikeWebApiDeclarations, toOas}
import amf.plugins.document.webapi.parser.spec.declaration.OasLikeSecuritySettingsParser
import amf.plugins.document.webapi.parser.spec.domain.{
  OasLikeEndpointParser,
  OasLikeOperationParser,
  OasLikeServerVariableParser,
  ParsingHelpers
}
import amf.plugins.domain.shapes.models.AnyShape
import amf.plugins.domain.webapi.models.security.SecurityScheme
import amf.plugins.domain.webapi.models.{EndPoint, Operation, Server, WebApi}
import org.yaml.model.{YMap, YMapEntry, YNode, YScalar}

import scala.collection.mutable

trait OasLikeSpecVersionFactory extends SpecVersionFactory {
  def serverVariableParser(entry: YMapEntry, parent: String): OasLikeServerVariableParser
  // TODO ASYNC complete this
  def operationParser(entry: YMapEntry, producer: String => Operation): OasLikeOperationParser
  def endPointParser(entry: YMapEntry, producer: String => EndPoint, collector: List[EndPoint]): OasLikeEndpointParser
  def securitySettingsParser(map: YMap, scheme: SecurityScheme): OasLikeSecuritySettingsParser
}

abstract class OasLikeWebApiContext(loc: String,
                                    refs: Seq[ParsedReference],
                                    options: ParsingOptions,
                                    private val wrapped: ParserContext,
                                    private val ds: Option[OasLikeWebApiDeclarations] = None,
                                    private val operationIds: mutable.Set[String] = mutable.HashSet())
    extends WebApiContext(loc, refs, options, wrapped, ds) {

  val factory: OasLikeSpecVersionFactory

  def makeCopy(): OasLikeWebApiContext
  private def makeCopyWithJsonPointerContext() = {
    val copy = makeCopy()
    copy.jsonSchemaRefGuide = this.jsonSchemaRefGuide
    copy
  }

  def isMainFileContext: Boolean = loc == jsonSchemaRefGuide.currentLoc

  override val declarations: OasLikeWebApiDeclarations =
    ds.getOrElse(
      new OasLikeWebApiDeclarations(
        refs
          .flatMap(
            r =>
              if (r.isExternalFragment)
                r.unit.asInstanceOf[ExternalFragment].encodes.parsed.map(node => r.origin.url -> node)
              else None)
          .toMap,
        None,
        errorHandler = eh,
        futureDeclarations = futureDeclarations
      ))

  override def link(node: YNode): Either[String, YNode] = {
    node.to[YMap] match {
      case Right(map) =>
        val ref: Option[String] = map.key("$ref").flatMap(v => v.value.asOption[YScalar]).map(_.text)
        ref match {
          case Some(url) => Left(url)
          case None      => Right(node)
        }
      case _ => Right(node)
    }
  }

  val linkTypes: Boolean = wrapped match {
    case _: RamlWebApiContext => false
    case _                    => true
  }

  val shapesThatDontPermitRef = List("paths")

  override def ignore(shape: String, property: String): Boolean =
    property.startsWith("x-") || (property == "$ref" && !shapesThatDontPermitRef.contains(shape)) || (property
      .startsWith("/") && shape == "paths")

  /** Used for accumulating operation ids.
    * returns true if id was not present, and false if operation being added is already present. */
  def registerOperationId(id: String): Boolean = operationIds.add(id)

  def navigateToRemoteYNode[T <: OasLikeWebApiContext](ref: String)(implicit ctx: T): Option[RemoteNodeNavigation[T]] = {
    val nodeOption = jsonSchemaRefGuide.obtainRemoteYNode(ref)
    val rootNode   = jsonSchemaRefGuide.getRootYNode(ref)
    nodeOption.map { node =>
      val newCtx = ctx.makeCopyWithJsonPointerContext().moveToReference(node.location.sourceName).asInstanceOf[T]
      rootNode.foreach(newCtx.setJsonSchemaAST)
      RemoteNodeNavigation(node, newCtx)
    }
  }

  def parseRemoteJSONPath(ref: String): Option[AnyShape] = {
    jsonSchemaRefGuide.withFragmentAndInFileReference(ref) { (fragment, referenceUrl) =>
      val newCtx = makeCopyWithJsonPointerContext().moveToReference(fragment.location().get)
      JsonSchemaPlugin.parseFragment(fragment, referenceUrl)(newCtx)
    }
  }

  def moveToReference(loc: String): this.type = {
    jsonSchemaRefGuide = jsonSchemaRefGuide.changeJsonSchemaSearchDestination(loc)
    this
  }

  override def autoGeneratedAnnotation(s: Shape): Unit = ParsingHelpers.oasAutoGeneratedAnnotation(s)
}

case class RemoteNodeNavigation[T <: OasLikeWebApiContext](remoteNode: YNode, context: T)

object RemoteNodeNavigation {

  def unapply[T <: OasLikeWebApiContext](arg: RemoteNodeNavigation[T]): Option[(YNode, T)] =
    Some((arg.remoteNode, arg.context))
}
