package timeserver

import zio.*
import java.time.ZoneId

trait TimeService:
  def getNow: UIO[GetNowResponse]
  def setTimezone(tz: Timezone): UIO[Unit]

object TimeService:
  val live: ULayer[TimeService] = ZLayer.fromZIO(
    ZIO.succeed {
      new TimeService:

        private var tz: Timezone = Timezone("Europe/Moscow")

        override def getNow: UIO[GetNowResponse] = Clock.instant.map(_.atZone(ZoneId.of(tz.value))).map(GetNowResponse(_))

        override def setTimezone(tz: Timezone): UIO[Unit] = ZIO.succeed(this.tz = tz)
    }
  )

