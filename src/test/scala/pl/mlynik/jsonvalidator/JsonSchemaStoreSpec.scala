package pl.mlynik.jsonvalidator

import zio.*
import zio.test.*
import zio.test.Assertion.*

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object JsonSchemaStoreSpec extends ZIOSpecDefault {

  val testSchema = JsonSchema("""{json: \"json\"}""")

  def removeFile(path: Path) =
    ZIO.attemptBlockingIO(Files.deleteIfExists(path))

  def spec = suite("JsonSchemaStore Spec")(
    test("saves file and load file") {
      for {
        jsonSchemaStoreLive <- ZIO.service[JsonSchemaStore]
        storedFilePath <- jsonSchemaStoreLive.store("test-schema", testSchema)
        loadedSchema <- jsonSchemaStoreLive.load("test-schema")
        _ <- removeFile(storedFilePath)
      } yield assert(loadedSchema)(equalTo(testSchema))
    },
    test("handles missing schema") {
      for {
        jsonSchemaStoreLive <- ZIO.service[JsonSchemaStore]
        error <- jsonSchemaStoreLive.load("test-schema").either

      } yield assert(error)(isLeft)
    },
    test("handles conflicting schema schema") {
      for {
        jsonSchemaStoreLive <- ZIO.service[JsonSchemaStore]
        storedFilePath <- jsonSchemaStoreLive.store("test-schema", testSchema)
        error <- jsonSchemaStoreLive.store("test-schema", testSchema).either
        _ <- removeFile(storedFilePath)
      } yield assert(error)(isLeft(equalTo(JsonStoreError.SchemaAlreadyExists)))
    }
  ).provide(
    JsonValidatorLive.layer,
    ZLayer.succeed(Paths.get(java.lang.System.getProperty("java.io.tmpdir"))),
    JsonSchemaStoreLive.layer
  )
}
