package timeserver

import io.circe.{Codec as JsonCodec, Decoder, Encoder}
import monix.newtypes.{HasBuilder, HasExtractor, NewtypeWrapped}
import sttp.tapir.Codec.PlainCodec
import sttp.tapir.{Codec, DecodeResult, Schema}

import java.time.ZonedDateTime

case class GetNowResponse(timestamp: ZonedDateTime) derives JsonCodec.AsObject
case class Visitor(userAgent: UserAgent) derives JsonCodec.AsObject

object Timezone extends NewtypeWrapped[String]
type Timezone = Timezone.Type

object UserAgent extends NewtypeWrapped[String]
type UserAgent = UserAgent.Type

given[T](using
         extractor: HasExtractor.Aux[T, String],
         builder: HasBuilder.Aux[T, String]
        ): PlainCodec[T] =
  Codec.string.mapDecode(s => DecodeResult.Value(builder.build(s).toOption.get))(extractor.extract(_))

given[T](using
         extractor: HasExtractor.Aux[T, String],
         builder: HasBuilder.Aux[T, String]
        ): JsonCodec[T] =
  given Encoder[T] = Encoder.encodeString.contramap(t => extractor.extract(t))

  given Decoder[T] = Decoder.decodeString.map(s => builder.build(s).toOption.get)

  JsonCodec.from(Decoder[T], Encoder[T])

given[T](using
         extractor: HasExtractor.Aux[T, String],
         builder: HasBuilder.Aux[T, String]
        ): Schema[T] =
  Schema.schemaForString.map[T](s => builder.build(s).toOption)(t => extractor.extract(t))
