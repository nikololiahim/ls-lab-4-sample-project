package timeserver

import sttp.tapir.*
import sttp.model.HeaderNames
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint.Full
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.ZServerEndpoint
import timeserver.MainService.*
import zio.*

import java.time.ZonedDateTime

final class MainService(
    timeService: TimeService,
    visitorService: VisitorService
):

  extension [E, A](either: Either[E, A])
    def leftMap[E1](f: E => E1): Either[E1, A] = either match
      case Left(value)  => Left(f(value))
      case Right(value) => Right(value)

    def leftVoid: Either[Unit, A] = leftMap(_ => ())

  val getNowServerEndpoint: ZServerEndpoint[Any, Any] =
    getNowEndpoint.serverLogic { case headers =>
      val userAgent = extractUserAgent(headers)
      (for
        timestamp <- Clock.instant
        _ <- visitorService.putVisitor(Visitor(userAgent, timestamp))
        now <- timeService.getNow
      yield now).either.map(_.leftVoid)
    }

  val getTimezoneServerEndpoint: ZServerEndpoint[Any, Any] =
    getTimezoneEndpoint.serverLogic(headers =>
      val userAgent = extractUserAgent(headers)
      (for
        timestamp <- Clock.instant
        _ <- visitorService.putVisitor(Visitor(userAgent, timestamp))
        tz <- timeService.getTimezone
      yield tz).either.map(_.leftVoid)
    )

  val setTimezoneServerEndpoint: ZServerEndpoint[Any, Any] =
    setTimezoneEndpoint.serverLogic { case (tz, headers) =>
      val userAgent = extractUserAgent(headers)
      (for
        timestamp <- Clock.instant
        _ <- visitorService.putVisitor(Visitor(userAgent, timestamp))
        _ <- timeService.setTimezone(tz)
      yield ()).either.map(_.leftVoid)
    }
  val getVisitorsServerEndpoint: ZServerEndpoint[Any, Any] =
    getVisitorsEndpoint.serverLogic(_ => visitorService.getVisitors.either.map(_.leftVoid))

  val apiEndpoints: List[ZServerEndpoint[Any, Any]] =
    List(getNowServerEndpoint, setTimezoneServerEndpoint, getTimezoneServerEndpoint, getVisitorsServerEndpoint)

  val docEndpoints: List[ZServerEndpoint[Any, Any]] = SwaggerInterpreter()
    .fromServerEndpoints[Task](apiEndpoints, "ls-lab-4-sample-project", "1.0.0")

  val prometheusMetrics: PrometheusMetrics[Task] = PrometheusMetrics.default[Task]()
  val metricsEndpoint: ZServerEndpoint[Any, Any] = prometheusMetrics.metricsEndpoint

  val all: List[ZServerEndpoint[Any, Any]] = apiEndpoints ++ docEndpoints ++ List(metricsEndpoint)

object MainService:

  private def extractUserAgent(headers: List[String]): UserAgent =
    UserAgent(headers.headOption.getOrElse("anonymous"))

  val live: URLayer[VisitorService & TimeService, MainService] = ZLayer.fromZIO {
    for
      timeService <- ZIO.service[TimeService]
      visitorService <- ZIO.service[VisitorService]
    yield MainService(timeService, visitorService)
  }

  val getNowEndpoint =
    endpoint.get
      .in("now")
      .in(header[List[String]](HeaderNames.UserAgent))
      .out(jsonBody[GetNowResponse])

  val setTimezoneEndpoint =
    endpoint.patch
      .in("timezone")
      .in(query[Timezone]("timezone"))
      .in(header[List[String]](HeaderNames.UserAgent))

  val getTimezoneEndpoint =
    endpoint.get
      .in("timezone")
      .in(header[List[String]](HeaderNames.UserAgent))
      .out(jsonBody[GetTimezoneResponse])

  val getVisitorsEndpoint =
    endpoint.get
      .in("visitors")
      .out(jsonBody[List[Visitor]])
