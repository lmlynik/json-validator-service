package pl.mlynik.jsonvalidator

import zio.*
import zio.test.*
import zio.test.Assertion.*

import java.nio.file.Paths

object JsonSchemaStoreSpec extends ZIOSpecDefault {

  val testSchema = JsonSchema("""{json: \"json\"}""")

  def spec = suite("JsonSchemaStore Spec")(
    test("saves file and load file") {
      for {
        jsonSchemaStoreLive <- ZIO.service[JsonSchemaStore]
        _ <- jsonSchemaStoreLive.store("test-schema", testSchema)
        loadedSchema <- jsonSchemaStoreLive.load("test-schema")

      } yield assert(loadedSchema)(equalTo(testSchema))
    },
    test("handles missing schema") {
      for {
        jsonSchemaStoreLive <- ZIO.service[JsonSchemaStore]
        error <- jsonSchemaStoreLive.load("test-schema").either

      } yield assert(error)(isLeft)
    }
  ).provide(
    JsonValidatorLive.layer,
    ZLayer.fromZIO(
      ZIO.attempt(Paths.get(java.lang.System.getProperty("java.io.tmpdir")))
    ),
    JsonSchemaStoreLive.layer
  )
}
