package pl.mlynik.jsonvalidator
import zio.*
import io.circe.*, io.circe.parser.*

enum JsonValidationError {
  case InvalidJson extends JsonValidationError
}

trait JsonValidator {
  def validateJson(json: String): IO[JsonValidationError, Unit]
}

final case class JsonValidatorLive() extends JsonValidator() {
  def validateJson(json: String): IO[JsonValidationError, Unit] = {
    ZIO
      .fromEither(parse(json))
      .orElseFail(JsonValidationError.InvalidJson)
      .unit
  }
}

object JsonValidatorLive {
  val layer: ULayer[JsonValidator] =
    ZLayer.fromFunction(JsonValidatorLive.apply _)
}
