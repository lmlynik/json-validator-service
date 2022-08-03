package pl.mlynik.jsonvalidator

import zio.*
import zio.test.*
import zio.test.Assertion.*

import java.nio.file.Paths

object JsonValidatorSpec extends ZIOSpecDefault {

  def spec = suite("JsonValidator Spec")(
    test("handles valid json") {
      for {
        jsonValidator <- ZIO.service[JsonValidator]
        res <- jsonValidator.validateJson("""{}""").either
      } yield assert(res)(isRight)
    },
    test("handles invalid json") {
      for {
        jsonValidator <- ZIO.service[JsonValidator]
        res <- jsonValidator.validateJson("""{/}""").either
      } yield assert(res)(isLeft(equalTo(JsonValidationError.InvalidJson)))
    }
  ).provide(JsonValidatorLive.layer)
}
