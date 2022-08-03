package pl.mlynik.jsonvalidator

import zio.*

import java.nio.file.{Files, OpenOption, Path, Paths, StandardOpenOption}
import java.nio.charset.StandardCharsets
import scala.io.Source

case class JsonSchema(content: String)

trait JsonSchemaStore {
  def store(id: String, schema: JsonSchema): Task[Unit]
  def load(id: String): Task[JsonSchema]
}

final case class JsonSchemaStoreLive(
    validator: JsonValidator,
    directoryPath: Path
) extends JsonSchemaStore {

  private def idWithExtension(id: String) =
    directoryPath.resolve(Paths.get(id + ".json"))

  private def writeFile(path: Path, content: String): Task[Path] = {
    ZIO.attempt(
      Files.write(
        path,
        content.getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE
      )
    )
  }

  private def readFile(path: Path): Task[String] =
    ZIO.attempt(Files.readString(path, StandardCharsets.UTF_8))

  def store(id: String, schema: JsonSchema): Task[Unit] = {
    writeFile(idWithExtension(id), schema.content).unit
  }

  def load(id: String): Task[JsonSchema] =
    readFile(idWithExtension(id)).map(JsonSchema.apply)
}

object JsonSchemaStoreLive {
  val layer: ZLayer[JsonValidator & Path, Nothing, JsonSchemaStore] =
    ZLayer.fromFunction(JsonSchemaStoreLive.apply _)
}
