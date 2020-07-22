package amf.plugins.document.webapi.parser.spec.common.emitters.factory

import amf.core.emitter.{PartEmitter, SpecOrdering}
import amf.core.errorhandling.UnhandledErrorHandler
import amf.core.model.domain.templates.AbstractDeclaration
import amf.core.model.domain.Shape
import amf.plugins.document.webapi.contexts.emitter.raml.{
  Raml08SpecEmitterContext,
  Raml10SpecEmitterContext,
  RamlSpecEmitterContext
}
import amf.plugins.document.webapi.parser.spec.declaration.{AbstractDeclarationPartEmitter, DataNodeEmitter}
import amf.plugins.document.webapi.parser.spec.declaration.emitters.raml.{Raml08TypePartEmitter, Raml10TypePartEmitter}
import amf.plugins.document.webapi.parser.spec.domain.{Raml08ResponsePartEmitter, Raml10ResponsePartEmitter}
import amf.plugins.domain.webapi.models.Response
import amf.plugins.domain.webapi.models.templates.{ResourceType, Trait}

object Raml10EmitterFactory extends RamlEmitterFactory {
  // TODO ajust error handler
  implicit val ctx: Raml10SpecEmitterContext = new Raml10SpecEmitterContext(UnhandledErrorHandler)

  override def typeEmitter(s: Shape): Option[PartEmitter] =
    Some(Raml10TypePartEmitter(s, SpecOrdering.Lexical, None, references = Nil))

  override def responseEmitter(e: Response): Option[PartEmitter] =
    Some(Raml10ResponsePartEmitter(e, SpecOrdering.Lexical, Nil))
}

object Raml08EmitterFactory extends RamlEmitterFactory {
  // TODO ajust error handler
  implicit val ctx: Raml08SpecEmitterContext = new Raml08SpecEmitterContext(UnhandledErrorHandler)

  override def typeEmitter(s: Shape): Option[PartEmitter] =
    Some(Raml08TypePartEmitter(s, SpecOrdering.Lexical, None, references = Nil))

  override def responseEmitter(e: Response): Option[PartEmitter] =
    Some(Raml08ResponsePartEmitter(e, SpecOrdering.Lexical, Nil))
}

trait RamlEmitterFactory extends DomainElementEmitterFactory {

  implicit val ctx: RamlSpecEmitterContext

  override def traitEmitter(t: Trait): Option[PartEmitter] = Some(abstractDeclarationEmitter(t))

  override def resourceTypeEmitter(r: ResourceType): Option[PartEmitter] = Some(abstractDeclarationEmitter(r))

  private def abstractDeclarationEmitter(a: AbstractDeclaration): PartEmitter =
    AbstractDeclarationPartEmitter(a, SpecOrdering.Lexical, Nil)
}
