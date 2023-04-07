package timeserver

import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.ConfigReader.Result
import pureconfig.generic.derivation.default.*
import scala.util.Try
import sttp.model.Uri
import sttp.model.Uri.*
import zio.*

case class Config(
    server: ServerConfig,
    database: Database,
    postgres: Postgres,
    time: TimeConfig
)

enum Database:
  case InMemory
  case Postgres

case class TimeConfig(
    defaultTimezone: Timezone
)

case class Postgres(
    host: Uri,
    password: String,
    username: String
)

case class ServerConfig(
    host: Uri,
    port: Int
)

given ConfigReader[Uri] = ConfigReader.fromStringTry(str => Try(uri"$str"))
given ConfigReader[Timezone] = ConfigReader.fromStringTry(str =>
  Try(java.time.ZoneId.of(str).toString)
    .map(Timezone.fromStringUnsafe(_))
)
given ConfigReader[ServerConfig] = ConfigReader.derived
given ConfigReader[Postgres] = ConfigReader.derived
given ConfigReader[TimeConfig] = ConfigReader.derived
given ConfigReader[Config] = ConfigReader.derived
given ConfigReader[Database] = ConfigReader.fromStringOpt {
  case "in-memory" => Some(Database.InMemory)
  case "postgres"  => Some(Database.Postgres)
  case _           => None
}

object Config {
  val live = ZLayer.fromZIO {
    for
      res <- ZIO.attemptBlocking(ConfigSource.default.load[Config])
      cfg <- res.fold(
        errs => ZIO.fail(new RuntimeException(errs.prettyPrint(2))),
        cfg => ZIO.succeed(cfg)
      )
    yield cfg
  }
}
