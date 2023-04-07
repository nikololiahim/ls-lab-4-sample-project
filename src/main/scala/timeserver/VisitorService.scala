package timeserver

import zio.*
import timeserver.Config

trait VisitorService:
  def getVisitors: Task[List[Visitor]]
  def putVisitor(visitor: Visitor): Task[Unit]

object VisitorService:
  val live = ZLayer.fromZIO(ZIO.service[Config]).flatMap {
    env =>
      val config = env.get[Config]
      config.database match
        case Database.InMemory => InMemoryVisitorServiceImpl.live
        case Database.Postgres => PostgresVisitorServiceImpl.live
  }

final class InMemoryVisitorServiceImpl(visitors: Ref[Chunk[Visitor]]) extends VisitorService:

  override def getVisitors: Task[List[Visitor]] =
    for
      visitors <- visitors.get
      _ <- Console
        .printLine(visitors)
        .mapError(new Throwable(_))
    yield visitors.toList

  override def putVisitor(visitor: Visitor): Task[Unit] =
    for visitors <- visitors.update(_.appended(visitor))
    yield ()


object InMemoryVisitorServiceImpl:
  val live = ZLayer.fromZIO(
    for ref <- Ref.make(Chunk.empty[Visitor])
    yield InMemoryVisitorServiceImpl(ref)
  )


final class PostgresVisitorServiceImpl extends VisitorService:
  override def getVisitors: Task[List[Visitor]] = ???
  override def putVisitor(visitor: Visitor): Task[Unit] = ???


object PostgresVisitorServiceImpl {
  val live: URLayer[Config, PostgresVisitorServiceImpl] = ???
}
