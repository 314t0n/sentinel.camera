package sentinel.router.messages

import sentinel.camera.camera.reader.BroadCastRunnableGraph

sealed trait Response

case class Ready(msg: String) extends Response

case class Error(reason: String) extends Response

case class Status(reason: Either[Throwable, String]) extends Response

case class SourceInit(broadCast: BroadCastRunnableGraph) extends Response
