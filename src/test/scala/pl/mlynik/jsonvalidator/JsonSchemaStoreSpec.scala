package pl.mlynik.jsonvalidator

import zio.Scope
import zio.test.*
import zio.test.Assertion.*

import java.nio.file.Paths

object JsonSchemaStoreSpec extends ZIOSpecDefault {
  val jsonSchemaStoreLive =
    JsonSchemaStoreLive.apply(Paths.get(System.getProperty("java.io.tmpdir")))

  val testSchema = JsonSchema("""{json: \"json\"}""")

  def spec = suite("JsonSchemaStore Spec")(
    test("saves file and load file") {
      for {
        _ <- jsonSchemaStoreLive.store("test-schema", testSchema)
        loadedSchema <- jsonSchemaStoreLive.load("test-schema")

      } yield assert(loadedSchema)(equalTo(testSchema))
    }
  )
}
