package timeserver

import io.circe.{Decoder, Encoder, Codec as JsonCodec}
import monix.newtypes.*
import sttp.tapir.Codec.PlainCodec
import sttp.tapir.{Codec, DecodeResult, Schema}

import scala.util.Try
import java.time.{Instant, ZonedDateTime}

case class GetNowResponse(localDateTime: ZonedDateTime) derives JsonCodec.AsObject
case class Visitor(userAgent: UserAgent, timestamp: Instant) derives JsonCodec.AsObject
case class GetTimezoneResponse(timezone: Timezone) derives JsonCodec.AsObject

type Timezone = Timezone.Type
object Timezone extends NewtypeValidated[String] {

  def fromStringUnsafe(value: String): Timezone = apply(value).getOrElse(
    throw new IllegalArgumentException(s""""$value" is not a timezone id!""")
  )

  override def apply(value: String): Either[BuildFailure[Timezone], Timezone] =
    Try(java.time.ZoneId.of(value).toString).fold(
      err => Left(BuildFailure(err.getMessage)),
      tz => Right(Timezone.unsafeCoerce(tz))
    )
}

object UserAgent extends NewtypeWrapped[String]
type UserAgent = UserAgent.Type

given [T](using
    extractor: HasExtractor.Aux[T, String],
    builder: HasBuilder.Aux[T, String],
    typeInfo: TypeInfo[T]
): PlainCodec[T] =
  Codec.string.mapDecode(s =>
    builder.build(s) match
      case Left(err)    => DecodeResult.Error(err.toReadableString, err.toException)
      case Right(value) => DecodeResult.Value(value)
  )(extractor.extract(_))

given [T](using
    extractor: HasExtractor.Aux[T, String],
    builder: HasBuilder.Aux[T, String],
    typeInfo: TypeInfo[T]
): JsonCodec[T] =
  given Encoder[T] = Encoder.encodeString.contramap(t => extractor.extract(t))
  given Decoder[T] = Decoder.decodeString.map(s => builder.build(s).toOption.get)
  JsonCodec.from(Decoder[T], Encoder[T])

given [T](using
    extractor: HasExtractor.Aux[T, String],
    builder: HasBuilder.Aux[T, String]
): Schema[T] =
  Schema.schemaForString.map[T](s => builder.build(s).toOption)(t => extractor.extract(t))
