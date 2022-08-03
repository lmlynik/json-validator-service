package pl.mlynik.jsonvalidator

import zio.*

import java.io.IOException
import java.nio.file.{Files, OpenOption, Path, Paths, StandardOpenOption}
import java.nio.charset.StandardCharsets
import scala.io.Source

case class JsonSchema(content: String)

object SchemaNotFound
object SchemaAlreadyExists

trait JsonSchemaStore {
  def store(id: String, schema: JsonSchema): IO[SchemaAlreadyExists.type, Unit]
  def load(id: String): IO[SchemaNotFound.type, JsonSchema]
}

final case class JsonSchemaStoreLive(
    validator: JsonValidator,
    directoryPath: Path
) extends JsonSchemaStore {

  private def idWithExtension(id: String) =
    directoryPath.resolve(Paths.get(id + ".json"))

  private def writeFile(path: Path, content: String): IO[IOException, Path] =
    ZIO.scoped {
      ZIO.attemptBlockingIO(
        Files.write(
          path,
          content.getBytes(StandardCharsets.UTF_8),
          StandardOpenOption.CREATE,
          StandardOpenOption.WRITE
        )
      )
    }

  private def readFile(path: Path): IO[IOException, String] = ZIO.scoped {
    ZIO.attemptBlockingIO(Files.readString(path, StandardCharsets.UTF_8))
  }

  def store(
      id: String,
      schema: JsonSchema
  ): IO[SchemaAlreadyExists.type, Unit] = ZIO.scoped {
    val path = idWithExtension(id)
    if (Files.exists(path)) {
      ZIO.fail(SchemaAlreadyExists)
    } else
      writeFile(idWithExtension(id), schema.content).unit.orDie
  }

  def load(id: String): IO[SchemaNotFound.type, JsonSchema] =
    readFile(idWithExtension(id)).mapBoth(_ => SchemaNotFound, JsonSchema.apply)
}

object JsonSchemaStoreLive {
  val layer: ZLayer[JsonValidator & Path, Nothing, JsonSchemaStore] =
    ZLayer.fromFunction(JsonSchemaStoreLive.apply _)
}
