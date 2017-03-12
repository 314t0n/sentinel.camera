package sentinel.camera.utils.transform

import org.bytedeco.javacpp.opencv_core._

/**
  * Created by Lloyd on 2/15/16.
  */

/**
  * Flipping is in a separate object because it has various directions
  */
object Flip {

  /**
    * Clones the image and returns a flipped version of the given image matrix along the y axis (horizontally)
    */
  def horizontal(mat: Mat): Mat = {
    val cloned = mat.clone()
    flip(cloned, cloned, 1)
    cloned
  }

}