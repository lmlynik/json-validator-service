package pl.mlynik.jsonvalidator

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
          // this can be any existing folder
          Paths.get(java.lang.System.getProperty("java.io.tmpdir"))
        )
      )
}
