package pl.mlynik.jsonvalidator

import zio.*

import java.io.IOException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.nio.charset.StandardCharsets
import scala.io.Source

case class JsonSchema(content: String)

enum JsonStoreError {
  case SchemaAlreadyExists extends JsonStoreError
}

enum JsonLoadError {
  case SchemaNotFound extends JsonLoadError
}

trait JsonSchemaStore {
  def store(id: String, schema: JsonSchema): IO[JsonStoreError, Path]
  def load(id: String): IO[JsonLoadError, JsonSchema]
}

final case class JsonSchemaStoreLive(
    validator: JsonValidator,
    directoryPath: Path
) extends JsonSchemaStore {

  private def idWithExtension(id: String) =
    ZIO.attempt(directoryPath.resolve(Paths.get(id + ".json"))).orDie

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
  ): IO[JsonStoreError, Path] = ZIO.scoped {
    for {
      path <- idWithExtension(id)
      _ <- ZIO.when(Files.exists(path))(
        ZIO.fail(JsonStoreError.SchemaAlreadyExists)
      )
      path <- writeFile(path, schema.content).orDie
    } yield path
  }

  def load(id: String): IO[JsonLoadError, JsonSchema] = {
    val loadedFile = for {
      path <- idWithExtension(id)
      file <- readFile(path)
    } yield file
    loadedFile.map(JsonSchema.apply).refineOrDie {
      case _: NoSuchFileException => JsonLoadError.SchemaNotFound
    }
  }
}

object JsonSchemaStoreLive {
  val layer: ZLayer[JsonValidator & Path, Nothing, JsonSchemaStore] =
    ZLayer.fromFunction(JsonSchemaStoreLive.apply _)
}
