package pl.mlynik.jsonvalidator

import zio.*

import java.io.IOException
import java.nio.file.{Files, OpenOption, Path, Paths, StandardOpenOption}
import java.nio.charset.StandardCharsets
import scala.io.Source

case class JsonSchema(content: String)

enum JsonStoreError{
  case SchemaAlreadyExists extends JsonStoreError
}

enum JsonLoadError {
  case SchemaNotFound extends JsonLoadError
}

trait JsonSchemaStore {
  def store(id: String, schema: JsonSchema): IO[JsonStoreError, Unit]
  def load(id: String): IO[JsonLoadError, JsonSchema]
}

final case class JsonSchemaStoreLive(
    validator: JsonValidator,
    directoryPath: Path
) extends JsonSchemaStore {

  private def idWithExtension(id: String) =
    directoryPath.resolve(Paths.get(id + ".json"))

  private def writeFile(path: Path, content: String): IO[IOException, Path] =
    ZIO.scoped {
      ZIO.log(s"writing to $path") *>
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
    ZIO.log(s"loading from $path") *>
    ZIO.attemptBlockingIO(Files.readString(path, StandardCharsets.UTF_8))
  }

  def store(
      id: String,
      schema: JsonSchema
  ): IO[JsonStoreError, Unit] = ZIO.scoped {
    val path = idWithExtension(id)
    if (Files.exists(path)) {
      ZIO.fail(JsonStoreError.SchemaAlreadyExists)
    } else
      writeFile(idWithExtension(id), schema.content).unit.orDie
  }

  def load(id: String): IO[JsonLoadError, JsonSchema] =
    readFile(idWithExtension(id)).mapBoth(_ => JsonLoadError.SchemaNotFound, JsonSchema.apply)
}

object JsonSchemaStoreLive {
  val layer: ZLayer[JsonValidator & Path, Nothing, JsonSchemaStore] =
    ZLayer.fromFunction(JsonSchemaStoreLive.apply _)
}
