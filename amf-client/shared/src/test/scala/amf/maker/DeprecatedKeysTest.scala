package amf.maker

import amf.compiler.CompilerTestBuilder
import amf.core.remote.RamlYamlHint
import amf.core.validation.SeverityLevels
import amf.facades.Validation
import amf.{ProfileName, Raml08Profile, RamlProfile}
import org.scalatest.AsyncFunSuite

import scala.concurrent.ExecutionContext

class DeprecatedKeysTest extends AsyncFunSuite with CompilerTestBuilder {
  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  private val basePath = "file://amf-client/shared/src/test/resources/maker/deprecatedwarnings/"

  case class FixtureResult(level: String, message: String)
  case class Fixture(name: String, file: String, profileName: ProfileName, results: Seq[FixtureResult])

  val fixture = List(
    Fixture(
      "deprecated schemas 10 warning",
      "schemas.raml",
      RamlProfile,
      Seq(
        FixtureResult(SeverityLevels.WARNING,
                      "'schemas' keyword it's deprecated for 1.0 version, should use 'types' instead"))
    ),
    Fixture(
      "deprecated schema 10 warning",
      "schema.raml",
      RamlProfile,
      Seq(
        FixtureResult(SeverityLevels.WARNING,
                      "'schema' keyword it's deprecated for 1.0 version, should use 'type' instead"))
    ),
    Fixture("schemas in 08 non warning", "schemas08.raml", Raml08Profile, Nil),
    Fixture("schema in 08 non warning", "schema08.raml", Raml08Profile, Nil)
  )

  fixture.foreach { f =>
    test("Test " + f.name) {
      for {
        validation <- Validation(platform)
        model      <- build(basePath + f.file, RamlYamlHint, validation = Option(validation))
        report     <- validation.validate(model, f.profileName)
      } yield {
        assert(report.conforms)
        assert(report.results.lengthCompare(f.results.length) == 0)
        assert(
          !report.results
            .zip(f.results)
            .map({
              case (result, fResult) =>
                assert(result.message.equals(fResult.message))
                assert(result.level.equals(fResult.level))
            })
            .exists(_ != succeed))
      }
    }

  }
}
