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

import java.nio.file.Paths

object Main extends ZIOAppDefault {

  override def run =
    ZIO
      .serviceWithZIO[JsonValidatorServer](_.start)
      .provide(
        JsonValidatorServer.layer,
        JsonValidatorLive.layer,
        JsonSchemaValidatorLive.layer,
        JsonSchemaStoreLive.layer,
        ZLayer.succeed(
          Paths.get(java.lang.System.getProperty("java.io.tmpdir"))
        ),
      )
}
