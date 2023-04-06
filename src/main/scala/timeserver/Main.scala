package timeserver

import org.slf4j.LoggerFactory
import sttp.tapir.server.interceptor.log.DefaultServerLog
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zio.*
import zio.http.*

trait ServerModule

object Main extends ZIOAppDefault:
  val log = LoggerFactory.getLogger(ZioHttpInterpreter.getClass.getName)

  val MainLayer = ZLayer.fromZIO {
    for
      mainService <- ZIO.service[MainService]
      server <- ZIO.service[Server]
      serverConfig <- ZIO.service[ServerConfig]
      serverOptions =
        ZioHttpServerOptions.customiseInterceptors
          .serverLog(
            DefaultServerLog[Task](
              doLogWhenReceived = msg => ZIO.succeed(log.debug(msg)),
              doLogWhenHandled = (msg, error) => ZIO.succeed(error.fold(log.debug(msg))(err => log.debug(msg, err))),
              doLogAllDecodeFailures = (msg, error) => ZIO.succeed(error.fold(log.debug(msg))(err => log.debug(msg, err))),
              doLogExceptions = (msg: String, ex: Throwable) => ZIO.succeed(log.debug(msg, ex)),
              noLog = ZIO.unit
            )
          )
          .metricsInterceptor(mainService.prometheusMetrics.metricsInterceptor())
          .options
      app = ZioHttpInterpreter(serverOptions).toHttp(mainService.all)
      port = sys.env.get("HTTP_PORT").flatMap(_.toIntOption).getOrElse(8080)
      actualPort <- Server.install(app.withDefaultErrorResponse)
      _ <- Console.printLine(s"Go to http://localhost:${actualPort}/docs to open SwaggerUI. Press ENTER key to exit.")
      _ <- Console.readLine
    yield new ServerModule {}
  }

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    ZIO.service[ServerModule].provide(
      MainLayer,
      ServerConfig.live,
      Server.live,
      MainService.live,
      TimeService.live,
      VisitorService.live
    ).exitCode
