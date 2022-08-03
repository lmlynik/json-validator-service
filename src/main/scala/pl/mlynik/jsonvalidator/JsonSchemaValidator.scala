package pl.mlynik.jsonvalidator

import com.github.fge.jsonschema.SchemaVersion
import zio.*
import com.github.fge.jsonschema.main.JsonSchemaFactory
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

enum JsonSchemaValidationError {
  case InvalidJson(error: String) extends JsonSchemaValidationError
  case ValidationFailure(error: String) extends JsonSchemaValidationError
}

trait JsonSchemaValidator {
  def validateAgainstSchema(
      schema: JsonSchema,
      json: String
  ): IO[JsonSchemaValidationError, Unit]
}

final case class JsonSchemaValidationLive(objectMapper: ObjectMapper)
    extends JsonSchemaValidator() {

  def validateAgainstSchema(
      schemaContent: JsonSchema,
      json: String
  ): IO[JsonSchemaValidationError, Unit] = {

    for {
      schemaContentNode <- objectMapper
        .readTreeM(schemaContent.content)
        .orElseFail(JsonSchemaValidationError.InvalidJson(""))
      schema <- ZIO
        .attempt(JsonSchemaFactory.byDefault().getJsonSchema(schemaContentNode))
        .orDie
      jsonNode <- objectMapper
        .readTreeM(json)
        .orElseFail(JsonSchemaValidationError.InvalidJson(""))
      result = schema.validate(jsonNode)
      _ <- ZIO.unless(result.isSuccess) {
        val errors = Chunk
          .fromJavaIterator(result.iterator())
          .map(_.getMessage)
          .mkString(",")
        ZIO.fail(JsonSchemaValidationError.ValidationFailure(errors))
      }
    } yield ()
  }
}

object JsonSchemaValidatorLive {
  val layer: ZLayer[ObjectMapper, Nothing, JsonSchemaValidator] =
    ZLayer.fromFunction(JsonSchemaValidationLive.apply _)
}
