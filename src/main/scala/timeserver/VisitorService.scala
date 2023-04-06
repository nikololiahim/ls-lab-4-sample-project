package timeserver

import zio.*

trait VisitorService:
  def getVisitors: Task[List[Visitor]]
  def putVisitor(visitor: Visitor): Task[Unit]

object VisitorService:
  val live: ULayer[VisitorService] = ZLayer.fromZIO(
    ZIO.succeed {
      new VisitorService:
        private val visitors = scala.collection.mutable.ArrayBuffer.empty[Visitor]

        override def getVisitors: Task[List[Visitor]] =
          Console.printLine(visitors.toList)
          .mapError(new Throwable(_))
          .map(_ => visitors.toList)

        override def putVisitor(visitor: Visitor): Task[Unit] = ZIO.succeed(visitors.addOne(visitor))
    }
  )
