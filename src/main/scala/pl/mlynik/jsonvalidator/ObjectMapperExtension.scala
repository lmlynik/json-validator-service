package pl.mlynik.jsonvalidator

import zio.*
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}

extension (objectMapper: ObjectMapper) {
  def readTreeM(json: String): Task[JsonNode] =
    ZIO.attempt(objectMapper.readTree(json))

  // adapted from https://stackoverflow.com/a/41507468
  // a little bit of of isolated mutability isn't bad, especially as we are working with Java API's here
  def readTreeAndCleanM(json: String): Task[JsonNode] = readTreeM(json).map {
    jsonNode =>
      def stripNulls(node: JsonNode): Unit = {
        import com.fasterxml.jackson.databind.JsonNode
        val it = node.iterator
        while ({
          it.hasNext
        }) {
          val child = it.next
          if (child.isNull) it.remove()
          else stripNulls(child)
        }
      }
      stripNulls(jsonNode)
      jsonNode

  }
}
