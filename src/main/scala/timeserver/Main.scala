package timeserver

import scribe.*
import scribe.handler.LogHandler
import sttp.tapir.server.interceptor.log.DefaultServerLog
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zio.*
import zio.http.*
import timeserver.Config

trait ServerModule

object Main extends ZIOAppDefault:

  Logger.root.clearHandlers().clearModifiers().withHandler(minimumLevel = Some(Level.Trace)).replace()

  val log = Logger(ZioHttpInterpreter.getClass.getName)
    .clearModifiers()
    .clearHandlers()
    .withHandler(minimumLevel = Some(Level.Trace))

  val MainServerConfig: URLayer[Config, ServerConfig] =
    ZLayer
      .service[Config]
      .map(env =>
        val config = env.get[Config]
        env.add(ServerConfig.default.binding(config.server.host.toString, config.server.port)).prune[ServerConfig]
      )

  val MainLayer = ZLayer.fromZIO {
    for
      mainService <- ZIO.service[MainService]
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
      actualPort <- Server.install(app.withDefaultErrorResponse)
      _ <- Console.printLine(
        s"Go to http://${serverConfig.address.getHostName}:$actualPort/docs to open SwaggerUI. Press ENTER key to exit."
      )
    yield new ServerModule {}
  }

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    MainLayer.launch
      .provide(
        MainServerConfig,
        Server.live,
        Config.live,
        MainService.live,
        TimeService.live,
        VisitorService.live
      )
