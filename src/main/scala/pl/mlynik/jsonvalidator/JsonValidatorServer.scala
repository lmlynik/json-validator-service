package pl.mlynik.jsonvalidator

import com.fasterxml.jackson.databind.ObjectMapper
import io.circe.generic.auto.*
import sttp.tapir.EndpointOutput.StatusCode
import sttp.tapir.Codec
import sttp.tapir.CodecFormat
import sttp.tapir.PublicEndpoint
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.ztapir.ZServerEndpoint
import zhttp.http.HttpApp
import sttp.tapir.generic.auto.*
import zio.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import zhttp.service.Server
import sttp.model.StatusCode as ModelStatusCode

final case class JsonValidatorServer(
    jsonSchemaValidator: JsonSchemaValidator,
    jsonSchemaStore: JsonSchemaStore
) {

  case class SuccessResponse(
      action: String,
      id: String,
      status: String = "success"
  )

  case class ErrorResponse(
      action: String,
      id: String,
      message: String,
      status: String = "error"
  )

  val schemaPostEndpoint
      : PublicEndpoint[(String, String), ErrorResponse, SuccessResponse, Any] =
    endpoint.post
      .in("schema" / path[String]("schemaId"))
      .in(stringBody)
      .errorOut(
        oneOf[ErrorResponse](
          oneOfVariant(
            statusCode(ModelStatusCode.Conflict).and(
              jsonBody[ErrorResponse]
                .description("schema with id exists")
            )
          )
        )
      )
      .out(jsonBody[SuccessResponse])

  val schemaGetEndpoint: PublicEndpoint[String, ErrorResponse, String, Any] =
    endpoint.get
      .in("schema" / path[String]("schemaId"))
      .errorOut(
        oneOf[ErrorResponse](
          oneOfVariant(
            statusCode(ModelStatusCode.NotFound).and(
              jsonBody[ErrorResponse]
                .description("schema not found")
            )
          )
        )
      )
      .out(stringBody)

  val validatePostEndpoint
      : PublicEndpoint[(String, String), ErrorResponse, SuccessResponse, Any] =
    endpoint.post
      .in("validate" / path[String]("schemaId"))
      .in(stringBody)
      .errorOut(jsonBody[ErrorResponse])
      .out(jsonBody[SuccessResponse])

  val schemaPostServerEndpoint: ZServerEndpoint[Any, Any] =
    schemaPostEndpoint.zServerLogic { case (schemaId, schemaJson) =>
      jsonSchemaStore
        .store(schemaId, JsonSchema(schemaJson))
        .mapBoth(
          error => ErrorResponse("uploadSchema", schemaId, error.toString),
          _ => SuccessResponse("uploadSchema", schemaId)
        )
    }

  val schemaGetServerEndpoint: ZServerEndpoint[Any, Any] =
    schemaGetEndpoint.zServerLogic { schemaId =>
      jsonSchemaStore
        .load(schemaId)
        .mapBoth(
          error => ErrorResponse("downloadSchema", schemaId, error.toString),
          jsonSchema => jsonSchema.content
        )
    }

  val validatePostServerEndpoint: ZServerEndpoint[Any, Any] =
    validatePostEndpoint.zServerLogic { case (schemaId, json) =>
      val logic = for {
        schema <- jsonSchemaStore.load(schemaId)
        _ <- jsonSchemaValidator.validateAgainstSchema(schema, json)
      } yield ()

      logic.mapBoth(
        error => ErrorResponse("validateDocument", schemaId, error.toString),
        _ => SuccessResponse("validateDocument", schemaId)
      )
    }

  // Docs
  val swaggerEndpoints: List[ZServerEndpoint[Any, Any]] =
    SwaggerInterpreter()
      .fromEndpoints[Task](
        List(schemaPostEndpoint, schemaGetEndpoint, validatePostEndpoint),
        "JSON Validation",
        "1.0"
      )

  private val routes = ZioHttpInterpreter().toHttp(
    List(
      schemaPostServerEndpoint,
      schemaGetServerEndpoint,
      validatePostServerEndpoint
    ) ++ swaggerEndpoints
  )

  val start: ZIO[Any, Throwable, Nothing] = Server.start(8080, routes)
}

object JsonValidatorServer {
  val layer: ZLayer[
    JsonSchemaValidator & JsonSchemaStore,
    Nothing,
    JsonValidatorServer
  ] =
    ZLayer.fromFunction(JsonValidatorServer.apply _)
}
