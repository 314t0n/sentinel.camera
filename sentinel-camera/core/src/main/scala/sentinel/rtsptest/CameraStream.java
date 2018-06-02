package sentinel.rtsptest;
//VideoStream

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.io.FileInputStream;
import java.util.Arrays;

import static org.bytedeco.javacpp.avutil.AV_PIX_FMT_BGR24;
import static org.bytedeco.javacpp.opencv_core.cvReleaseData;
import static org.bytedeco.javacpp.opencv_core.sub8s;
import static org.bytedeco.javacpp.opencv_imgcodecs.cvEncodeImage;

public class CameraStream {

    FFmpegFrameGrabber grabber;
    FileInputStream fis; //video file
    int frame_nb; //current frame nb

    //-----------------------------------
    //constructor
    //-----------------------------------
    public CameraStream(int frameRate) throws Exception {
        //init variables

        grabber = new FFmpegFrameGrabber("video=Trust Webcam");
        grabber.setFormat("dshow");

//        grabber = new FFmpegFrameGrabber("/dev/video0");
//        grabber.setFormat("video4linux2");

        grabber.setImageWidth(640);
        grabber.setImageHeight(480);
        grabber.setFrameRate(frameRate);
        grabber.setNumBuffers(frameRate);
        grabber.setPixelFormat(AV_PIX_FMT_BGR24);
        grabber.start();
        frame_nb = 0;
    }

    OpenCVFrameConverter.ToIplImage converterToIplImage = new OpenCVFrameConverter.ToIplImage();

    public void stop() {
        try {
            grabber.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //-----------------------------------
    // getnextframe
    //returns the next frame as an array of byte and the size of the frame
    //-----------------------------------
    public int getnextframe(byte[] frame) throws Exception {
        opencv_core.IplImage image = converterToIplImage.convert(grabber.grab());

        byte[] frame_length = toBytes(image, ".jpg");

        System.arraycopy(frame_length, 0, frame, 0, frame_length.length);

        return frame_length.length;
    }

    public byte[] toBytes(opencv_core.IplImage image, String format) {
        if (!format.startsWith(".")) format = "." + format;

        opencv_core.CvMat m = cvEncodeImage(format, image.asCvMat());
        BytePointer bytePointer = m.data_ptr();

        byte[] imageData = new byte[m.size()];
        bytePointer.get(imageData, 0, m.size());
        cvReleaseData(m);

        return imageData;
    }
}