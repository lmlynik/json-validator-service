package pl.mlynik.jsonvalidator

import com.github.fge.jsonschema.SchemaVersion
import zio.*
import com.github.fge.jsonschema.main.JsonSchemaFactory
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jsonschema.core.report.ProcessingReport

enum JsonSchemaValidationError {
  case InvalidJson extends JsonSchemaValidationError
  case ValidationFailure(error: String) extends JsonSchemaValidationError
  case SchemaError extends JsonSchemaValidationError
}

trait JsonSchemaValidator {
  def validateAgainstSchema(
      schema: JsonSchema,
      json: String
  ): IO[JsonSchemaValidationError, Unit]
}

final case class JsonSchemaValidationLive()
    extends JsonSchemaValidator() {

  private val objectMapper = ZIO.succeed(new ObjectMapper())
  
  def validateAgainstSchema(
      schemaContent: JsonSchema,
      json: String
  ): IO[JsonSchemaValidationError, Unit] = {

    for {
      schema <- prepareSchema(schemaContent)
      objMapper <- objectMapper
      jsonNode <- objMapper
        .readTreeAndCleanM(json)
        .orElseFail(JsonSchemaValidationError.InvalidJson)
      result = schema.validate(jsonNode)
      _ <- ZIO.unless(result.isSuccess) {
        buildErrorResponse(result)
      }
    } yield ()
  }

  private def prepareSchema(schemaContent: JsonSchema) = (for {
    objMapper <- objectMapper
    schemaContentNode <- objMapper
      .readTreeM(schemaContent.content)
    schema <- ZIO
      .attempt(JsonSchemaFactory.byDefault().getJsonSchema(schemaContentNode))
  } yield schema).orElseFail(JsonSchemaValidationError.SchemaError)

  private def buildErrorResponse(result: ProcessingReport) = {
    val errors = Chunk
      .fromJavaIterator(result.iterator())
      .map(_.getMessage)
      .mkString(",")
    ZIO.fail(JsonSchemaValidationError.ValidationFailure(errors))
  }
}

object JsonSchemaValidatorLive {
  val layer: ULayer[ JsonSchemaValidator] =
    ZLayer.fromFunction(JsonSchemaValidationLive.apply _)
}
