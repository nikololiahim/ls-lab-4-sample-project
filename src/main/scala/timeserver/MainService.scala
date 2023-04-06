package timeserver

import sttp.tapir.*
import sttp.model.HeaderNames
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint.Full
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.ZServerEndpoint
import timeserver.MainService.{getNowEndpoint, getVisitorsEndpoint, patchNowEndpoint}
import zio.{Task, UIO, ZIO, ZLayer}

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
    getNowEndpoint.serverLogic { case (headers) =>
      val userAgent = UserAgent(headers.headOption.getOrElse("anonymous"))
      (for
        _ <- visitorService.putVisitor(Visitor(userAgent))
        now <- timeService.getNow
      yield now).either.map(_.leftVoid)
    }
  val patchNowServerEndpoint: ZServerEndpoint[Any, Any] =
    patchNowEndpoint.serverLogic { case (tz, headers) =>
      val userAgent = UserAgent(headers.headOption.getOrElse("anonymous"))
      (for
        _ <- visitorService.putVisitor(Visitor(userAgent))
        _ <- timeService.setTimezone(tz)
      yield ()).either.map(_.leftVoid)
    }
  val getVisitorsServerEndpoint: ZServerEndpoint[Any, Any] =
    getVisitorsEndpoint.serverLogic(_ => visitorService.getVisitors.either.map(_.leftVoid))

  val apiEndpoints: List[ZServerEndpoint[Any, Any]] = List(getNowServerEndpoint, patchNowServerEndpoint, getVisitorsServerEndpoint)

  val docEndpoints: List[ZServerEndpoint[Any, Any]] = SwaggerInterpreter()
    .fromServerEndpoints[Task](apiEndpoints, "ls-lab-4-sample-project", "1.0.0")

  val prometheusMetrics: PrometheusMetrics[Task] = PrometheusMetrics.default[Task]()
  val metricsEndpoint: ZServerEndpoint[Any, Any] = prometheusMetrics.metricsEndpoint

  val all: List[ZServerEndpoint[Any, Any]] = apiEndpoints ++ docEndpoints ++ List(metricsEndpoint)

object MainService:

  val live = ZLayer.fromZIO {
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

  val patchNowEndpoint =
    endpoint.patch
      .in("now")
      .in(query[Timezone]("timezone"))
      .in(header[List[String]](HeaderNames.UserAgent))

  val getVisitorsEndpoint =
    endpoint.get
      .in("visitors")
      .out(jsonBody[List[Visitor]])
