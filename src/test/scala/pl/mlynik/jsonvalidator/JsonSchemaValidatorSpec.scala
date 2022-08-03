package pl.mlynik.jsonvalidator

import com.fasterxml.jackson.databind.ObjectMapper
import zio.*
import zio.test.*
import zio.test.Assertion.*

import java.nio.file.Paths

object JsonSchemaValidatorSpec extends ZIOSpecDefault {

  val schema = JsonSchema("""
      |{
      |  "$schema": "http://json-schema.org/draft-04/schema#",
      |  "type": "object",
      |  "properties": {
      |    "source": {
      |      "type": "string"
      |    },
      |    "destination": {
      |      "type": "string"
      |    },
      |    "timeout": {
      |      "type": "integer",
      |      "minimum": 0,
      |      "maximum": 32767
      |    },
      |    "chunks": {
      |      "type": "object",
      |      "properties": {
      |        "size": {
      |          "type": "integer"
      |        },
      |        "number": {
      |          "type": "integer"
      |        }
      |      },
      |      "required": ["size"]
      |    }
      |  },
      |  "required": ["source", "destination"]
      |}
      |""".stripMargin)

  def spec = suite("JsonSchemaValidator Spec")(
    test("validated json and returns success") {
      for {
        jsonValidator <- ZIO.service[JsonSchemaValidator]
        res <- jsonValidator
          .validateAgainstSchema(
            schema,
            """
            |{
            |  "source": "/home/alice/image.iso",
            |  "destination": "/mnt/storage",
            |  "chunks": {
            |    "size": 1024
            |  }
            |}
            |""".stripMargin
          )
          .either
      } yield assert(res)(isRight)
    },
    test("validated json and returns the error - invalid type") {
      for {
        jsonValidator <- ZIO.service[JsonSchemaValidator]
        res <- jsonValidator
          .validateAgainstSchema(
            schema,
            """
            |{
            |  "source": "/home/alice/image.iso",
            |  "destination": "/mnt/storage",
            |  "chunks": {
            |    "size": 1024,
            |    "number": null
            |  }
            |}
            |""".stripMargin
          )
          .either
      } yield assert(res)(
        isLeft(
          equalTo(
            JsonSchemaValidationError.ValidationFailure(error =
              "instance type (null) does not match any allowed primitive type (allowed: [\"integer\"])"
            )
          )
        )
      )
    },
    test("validated json and returns the error - missing required") {
      for {
        jsonValidator <- ZIO.service[JsonSchemaValidator]
        res <- jsonValidator
          .validateAgainstSchema(
            schema,
            """
            |{
            |  "destination": "/mnt/storage",
            |  "chunks": {
            |    "size": 1024
            |  }
            |}
            |""".stripMargin
          )
          .either
      } yield assert(res)(
        isLeft(
          equalTo(
            JsonSchemaValidationError.ValidationFailure(error =
              "object has missing required properties ([\"source\"])"
            )
          )
        )
      )
    }
  ).provide(
    ZLayer.succeed(new ObjectMapper()),
    JsonSchemaValidatorLive.layer
  )
}
