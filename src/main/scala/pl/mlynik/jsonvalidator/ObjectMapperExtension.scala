package pl.mlynik.jsonvalidator

import zio.*
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}

extension (objectMapper: ObjectMapper) {
  def readTreeM(json: String): Task[JsonNode] =
    ZIO.attempt(objectMapper.readTree(json))
}
