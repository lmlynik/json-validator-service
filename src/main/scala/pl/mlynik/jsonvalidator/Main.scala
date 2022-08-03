package pl.mlynik.jsonvalidator

import com.fasterxml.jackson.databind.ObjectMapper
import io.circe.generic.auto.*
import sttp.tapir.EndpointOutput.StatusCode
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
import sttp.model.{StatusCode => ModelStatusCode}

import java.nio.file.Paths

object Main extends ZIOAppDefault {
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
            statusCode(ModelStatusCode.Conflict) and jsonBody[ErrorResponse]
              .description("schema with id exists")
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
            statusCode(ModelStatusCode.NotFound) and jsonBody[ErrorResponse]
              .description("schema not found")
          )
        )
      )
      .out(stringBody)
      .out(header("Content-Type", "application/json"))

  val validatePostEndpoint
      : PublicEndpoint[(String, String), ErrorResponse, SuccessResponse, Any] =
    endpoint.post
      .in("validate" / path[String]("schemaId"))
      .in(stringBody)
      .errorOut(jsonBody[ErrorResponse])
      .out(jsonBody[SuccessResponse])

  override def run = {
    val app = for {
      jsonSchemaStore <- ZIO.service[JsonSchemaStore]
      jsonSchemaValidator <- ZIO.service[JsonSchemaValidator]
      routes = {
        val schemaPostServerEndpoint: ZServerEndpoint[Any, Any] =
          schemaPostEndpoint.zServerLogic { case (schemaId, schemaJson) =>
            jsonSchemaStore
              .store(schemaId, JsonSchema(schemaJson))
              .mapBoth(
                error =>
                  ErrorResponse("uploadSchema", schemaId, error.toString),
                _ => SuccessResponse("uploadSchema", schemaId)
              )
          }

        val schemaGetServerEndpoint: ZServerEndpoint[Any, Any] =
          schemaGetEndpoint.zServerLogic { schemaId =>
            jsonSchemaStore
              .load(schemaId)
              .mapBoth(
                error =>
                  ErrorResponse("downloadSchema", schemaId, error.toString),
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
              error =>
                ErrorResponse("validateDocument", schemaId, error.toString),
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

        ZioHttpInterpreter().toHttp(
          List(
            schemaPostServerEndpoint,
            schemaGetServerEndpoint,
            validatePostServerEndpoint
          ) ++ swaggerEndpoints
        )
      }
      _ <- Server.start(8080, routes)
    } yield ()
    app
      .provide(
        JsonValidatorLive.layer,
        JsonSchemaValidatorLive.layer,
        ZLayer.fromZIO(
          ZIO.attempt(Paths.get(java.lang.System.getProperty("java.io.tmpdir")))
        ),
        JsonSchemaStoreLive.layer,
        ZLayer.succeed(new ObjectMapper())
      )
      .exitCode
  }
}
