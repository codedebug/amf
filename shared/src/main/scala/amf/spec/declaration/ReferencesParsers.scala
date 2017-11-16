package amf.spec.declaration

import amf.compiler.ParsedReference
import amf.document.Fragment.Fragment
import amf.document.{BaseUnit, DeclaresModel, Document}
import amf.domain.Annotation.Aliases
import amf.domain.dialects.DomainEntity
import amf.parser.YMapOps
import amf.spec.ParserContext
import org.yaml.model.YMap

import scala.collection.mutable

/**
  *
  */
object ReferenceDeclarations {
  def apply(references: mutable.Map[String, BaseUnit])(implicit ctx: ParserContext) =
    new ReferenceDeclarations(references)

  def apply()(implicit ctx: ParserContext): ReferenceDeclarations = apply(mutable.Map[String, BaseUnit]())
}

case class ReferenceDeclarations(val references: mutable.Map[String, BaseUnit] = mutable.Map(),
                                 declarations: Declarations)(implicit ctx: ParserContext) {


  def +=(alias: String, unit: BaseUnit): Unit = {
    references += (alias -> unit)
    val library = ctx.declarations.getOrCreateLibrary(alias)
    // todo : ignore domain entities of vocabularies?
    unit match {
      case d: DeclaresModel =>
        d.declares
          .filter({
            case _: DomainEntity => false
            case _               => true
          })
          .foreach(library += _)
    }
  }

  def +=(url: String, fragment: Fragment): Unit = {
    references += (url       -> fragment)
    ctx.declarations += (url -> fragment)
  }

  def +=(url: String, fragment: Document): Unit = references += (url -> fragment)

  def solvedReferences(): Seq[BaseUnit] = references.values.toSet.toSeq
}

case class ReferencesParser(key: String, map: YMap, references: Seq[ParsedReference])(implicit ctx: ParserContext) {
  def parse(location: String): ReferenceDeclarations = {
    val result: ReferenceDeclarations = parseLibraries(location)

    references.foreach {
      case ParsedReference(f: Fragment, s: String) => result += (s, f)
      case ParsedReference(d: Document, s: String) => result += (s, d)
      case _                                       =>
    }

    result
  }

  private def target(url: String): Option[BaseUnit] =
    references.find(r => r.parsedUrl.equals(url)).map(_.baseUnit)

  private def parseLibraries(id: String): ReferenceDeclarations = {
    val result = ReferenceDeclarations()

    map.key(
      key,
      entry =>
        entry.value
          .as[YMap]
          .entries
          .foreach(e => {
            val alias: String = e.key
            val url: String   = e.value
            target(url).foreach {
              case module: DeclaresModel => result += (alias, addAlias(module, alias)) // this is
              case other =>
                ctx
                  .violation(id, s"Expected module but found: $other", e) // todo Uses should only reference modules...
            }
          })
    )

    result
  }

  private def addAlias(module: BaseUnit, alias: String): BaseUnit = {
    module.annotations.find(classOf[Aliases]) match {
      case Some(aliases) =>
        module.annotations.reject(_.isInstanceOf[Aliases])
        module.add(aliases.copy(aliases = aliases.aliases + alias))
      case None => module.add(Aliases(Set(alias)))
    }
  }
}
