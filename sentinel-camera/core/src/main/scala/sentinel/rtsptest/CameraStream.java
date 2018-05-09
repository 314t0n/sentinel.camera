package sentinel.rtsptest;
//VideoStream

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.io.*;

import static org.bytedeco.javacpp.avutil.AV_PIX_FMT_BGR24;
import static org.bytedeco.javacpp.opencv_core.cvReleaseData;
import static org.bytedeco.javacpp.opencv_imgcodecs.CV_IMWRITE_JPEG_QUALITY;
import static org.bytedeco.javacpp.opencv_imgcodecs.cvEncodeImage;

public class VideoStream {

    FFmpegFrameGrabber grabber;
    FileInputStream fis; //video file
    int frame_nb; //current frame nb

    //-----------------------------------
    //constructor
    //-----------------------------------
    public VideoStream(String filename) throws Exception {

        //init variables

        grabber = new FFmpegFrameGrabber("video=Trust Webcam");
        grabber.setFormat("dshow");
        grabber.setFrameRate(5);
        grabber.setNumBuffers(5);
        grabber.setPixelFormat(AV_PIX_FMT_BGR24);
        grabber.start();
        frame_nb = 0;
    }

    OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
    OpenCVFrameConverter.ToIplImage converterToIplImage = new OpenCVFrameConverter.ToIplImage();


    public static void main(String... args) {
        try {
            VideoStream vs = new VideoStream("fosot0");
            int i = 10;
            while (i > 0) {
                i--;
                vs.getnextframe(new byte[1]);
                Thread.sleep(50);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //-----------------------------------
    // getnextframe
    //returns the next frame as an array of byte and the size of the frame
    //-----------------------------------
    public int getnextframe(byte[] frame) throws Exception {
        int length = 0;
        String length_string;
//        byte[] frame_length = new byte[5];

        //read current frame length
//        fis.read(frame_length,0,5);

        Frame frame1 = grabber.grab();

        opencv_core.Mat mat = converterToMat.convert(frame1);
        opencv_core.IplImage image = converterToIplImage.convert(frame1);

        byte[] frame_length = toBytes(image, ".jpg");

        System.out.println(frame_length.length);

        //transform frame_length to integer
//        length_string = new String(frame_length);
//        length = Integer.parseInt(length_string);

        return 1;
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