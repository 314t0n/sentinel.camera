package sentinel

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.event.WindowAdapter
import java.io.{File, ObjectInputStream}
import java.net.ServerSocket
import java.time.LocalDateTime
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.swing.JFrame.EXIT_ON_CLOSE
import javax.swing.JLabel
import javax.swing.JPanel

import com.typesafe.scalalogging.LazyLogging
import org.bytedeco.javacpp._
import org.bytedeco.javacv.CanvasFrame
import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.OpenCVFrameConverter
import sentinel.camera.camera.CameraFrame
import sentinel.communication.SocketFrame
import sentinel.communication.SocketFrameConverter

object POCServer extends App with LazyLogging {

  avformat.av_register_all()

  val mainPanel               = new JPanel()
  val framesQueueCounterLabel = new JLabel()
  val frameAddedLabel         = new JLabel()
  val fpsCounterLabel         = new JLabel()
  val frameDateLabel          = new JLabel()
  val statusLabel             = new JLabel()

  val converter            = new OpenCVFrameConverter.ToIplImage()
  val converterMat            = new OpenCVFrameConverter.ToMat()
  val sockerFrameConverter = new SocketFrameConverter()
  val server               = new ServerSocket(9999)
  var running              = true
  val scheduler            = new ScheduledThreadPoolExecutor(1)

  private val refreshRate = 200

  case class CanvasFrameSpecial(frame: Frame, date: LocalDateTime)

  val frames = new LinkedBlockingQueue[CanvasFrameSpecial]()
  val canvas: CanvasFrame = createCanvas({
    running = false
    server.close()
    System.exit(0)
  })

  val task = new Runnable {
    def run() = showImage
  }
//  val scheduledFuture = scheduler.scheduleAtFixedRate(task, 0, refreshRate, TimeUnit.MILLISECONDS)

  class MyTask extends TimerTask {
    override def run(): Unit = {
      framesQueueCounterLabel.setText(s"Frames: ${frames.size()}")
      if (!frames.isEmpty) {
        val frame = frames.take()
        frameDateLabel.setText(s"Frame date: ${frame.date.toString}")
        canvas.showImage(frame.frame)

//        val file = new File(s"${frame.date.toString}.jpg")
//        opencv_imgcodecs.imwrite(file.getAbsolutePath, converterMat.convert(frame.frame))
//        opencv_imgcodecs.cvSaveImage(file.getAbsolutePath, converter.convert(frame.frame))
//        println(s"file saved to ${file.getAbsolutePath}")

      }
      fpsCounterLabel.setText(s"FPS: ${1000 / refreshRate} / Refresh rate: $refreshRate ${LocalDateTime.now.toString}")
    }
  }

  val timerTask = new MyTask()
  //running timer task as daemon thread
  val timer = new Timer(true)
  timer.scheduleAtFixedRate(timerTask, 0, refreshRate)

  def showImage() = {
    framesQueueCounterLabel.setText(s"Frames: ${frames.size()}")
    if (!frames.isEmpty) {
      val frame = frames.take()
      frameDateLabel.setText(s"Frame date: ${frame.date.toString}")
      canvas.showImage(frame.frame)

      opencv_imgcodecs.imwrite(s"c:\\Kaszperek\\dev\\projects\\sentinel v2\\src\\asd\\${frame.date.toString}.jpg", converterMat.convert(frame.frame))

    }
    fpsCounterLabel.setText(s"FPS: ${1000 / refreshRate} / Refresh rate: $refreshRate ${LocalDateTime.now.toString}")
  }

  def toCanvasFrameSpecial(cf: SocketFrame): CanvasFrameSpecial =
    CanvasFrameSpecial(converter.convert(sockerFrameConverter.convert(cf)), cf.date)

  while (running) {
    statusLabel.setText("Server waiting for clients...")
    val serverSocket = server.accept()
    val in           = new ObjectInputStream(serverSocket.getInputStream())

    try {
      val next = in.readObject().asInstanceOf[Seq[SocketFrame]]
      frameAddedLabel.setText(s"Added ${next.size} frames.")
      next.map(toCanvasFrameSpecial).foreach(f => frames.put(f))
    } catch {
      case e: Exception => logger.error(e.getMessage, e)
    } finally serverSocket.close()
  }

  timer.cancel()
//  scheduledFuture.cancel(false)

  private def createCanvas(shutdown: => Unit): CanvasFrame = {
    val canvas = new CanvasFrame("Sentinel Camera View Util")
    canvas.setDefaultCloseOperation(EXIT_ON_CLOSE)
    canvas.setLayout(new GridLayout(1, 0))
    canvas.getContentPane().add(mainPanel, BorderLayout.CENTER)
    canvas.setSize(new Dimension(640, 480))
    canvas.setVisible(true)
    val layout = new GridLayout(0, 1)
    mainPanel.setLayout(layout)
    mainPanel.add(framesQueueCounterLabel)
    mainPanel.add(frameAddedLabel)
    mainPanel.add(fpsCounterLabel)
    mainPanel.add(frameDateLabel)
    mainPanel.add(statusLabel)
    canvas.addWindowListener(new WindowAdapter() {
      override def windowClosing(windowEvent: java.awt.event.WindowEvent): Unit = {
        logger.debug("Canvas close")
        shutdown
      }
    })
    canvas
  }
}
