package sentinel.graph

trait GraphFactory[A] {

  def createGraph: A

}
