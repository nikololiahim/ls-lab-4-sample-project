package timeserver

import zio.*
import java.time.ZoneId
import timeserver.Config

trait TimeService:
  def getNow: UIO[GetNowResponse]
  def setTimezone(tz: Timezone): UIO[Unit]
  def getTimezone: UIO[GetTimezoneResponse]

object TimeService:
  val live: URLayer[Config, TimeService] = ZLayer.fromZIO {
    for
      config <- ZIO.service[Config]
      defaultTimezone <- Ref.make(config.time.defaultTimezone)
    yield TimeServiceImpl(defaultTimezone)
  }

final class TimeServiceImpl(timezone: Ref[Timezone]) extends TimeService:
  override def getNow: UIO[GetNowResponse] =
    for
      tz <- getTimezone.map(_.timezone)
      now <- Clock.instant.map(_.atZone(ZoneId.of(tz.value)))
    yield GetNowResponse(now)

  override def setTimezone(tz: Timezone): UIO[Unit] = timezone.set(tz)

  override def getTimezone: UIO[GetTimezoneResponse] = timezone.get.map(GetTimezoneResponse(_))
