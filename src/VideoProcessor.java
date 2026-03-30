import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.video.TrackerMIL;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.util.ArrayList;

public class VideoProcessor {
    //we have to use static blocks bc this is basically a C++ wrapper eugh
    static {
        try{
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        }catch (Exception e){
            //TODO: REPLACE WITH BETTER, MORE SECURE, AND MORE ROBUST LOGGING
            e.printStackTrace();
        }
    }
    /*
        essentially, this is going to get run everytime a VideoProcessor obj
        is instantiated. otherwise, the JVM won't know that what it needs to do is
        go find a binary file to have the JVM run. the NATIVE_LIBRARY_NAME is just
        a constant that has the name of the file the JVM needs to find.
     */

    static void main(String[] args) {
        //need to force DirectShow here
        VideoCapture camera = new VideoCapture(0, Videoio.CAP_DSHOW);

        if (!camera.isOpened()) {
            System.out.println("Error: Camera could not be opened in Main.");
            return;
        }

        String haarXMLPath = "resources/haarcascade_frontalface_default.xml";
        CascadeClassifier detector = new CascadeClassifier(haarXMLPath);
        if(detector.empty()){
            System.out.println("Error opening XML file.");
            return;
        }

        Mat frame = new Mat();
        System.out.println("Camera opened.");

        int noRows = frame.rows();
        int noCols = frame.cols();
        int noChannels = frame.channels(); //should be 3 for us
        /*
         * i love how half of the docstrings for functions is just whatever c++ function
         * it's running, parameters included. like literally a line of code.
         * for Mat.channels() it says "int Mat::channels()" its like oh ok yeah i
         * didn't know that's what it did.
         */
        /*
         * or alternatively if you go to the javadocs it my say something like this:
         * "public int get(int row, int col, double[] data)"
         * and its like yeah OK that's the method signature cool but what does it DO??
         * HOW does it do it?? are there any other things i should know about??
         */

        ArrayList<double[]> values = new ArrayList<>();

        fifteenCapture(camera, frame, values, detector);

        camera.release();

        //annoyingly i *think* bc OpenCV is just c++ theres some issue with not
        //properly terminating the process once the end of the main method is reached
        //so i just solved it by force exiting.
        System.exit(0);
    }

    public static void fifteenCapture(VideoCapture camera, Mat frame, ArrayList<double[]> values, CascadeClassifier detector){
        long startTime = 0;
        long duration = 15000; //in ms
        double[] data;

        while (true) {
            if (camera.read(frame) && !frame.empty()) {
                //read and store values
                data = meanPixelValue(frame);
                values.add(data);

                MatOfRect faces = new MatOfRect();
                detector.detectMultiScale(frame, faces);
                //detection loop
                for(Rect face : faces.toArray()){
                    //defining our face rectangle
                    Imgproc.rectangle(frame, face, new Scalar(255,255,255),1);

                    Rect forehead = new Rect(
                            face.x + (int)(face.width*0.3),
                            face.y + (int)(face.height*0.1),
                            (int)(face.width*0.4),
                            (int)(face.height*0.15)
                    );

                    Rect leftCheek = new Rect(
                            face.x + (int)(face.width*0.15),
                            face.y + (int)(face.height*0.55),
                            (int)(face.width*0.2),
                            (int)(face.height*0.2)
                    );

                    Rect rightCheek = new Rect(
                            face.x + (int)(face.width*0.65),
                            face.y + (int)(face.height*0.55),
                            (int)(face.width * 0.2),
                            (int)(face.height * 0.2)
                    );

                    //draw rects
                    Imgproc.rectangle(frame, forehead, new Scalar(0,255,0),2);
                    Imgproc.rectangle(frame, leftCheek, new Scalar(0,255,0),2);
                    Imgproc.rectangle(frame, rightCheek, new Scalar(0,255,0),2);
                }
                HighGui.imshow("Main Camera Feed", frame);
            }

            // waiting 30ms to render just incase
            if (HighGui.waitKey(30) == 27) {
                break;
            }

            //start timer
            if(startTime <= 0){
                startTime = System.currentTimeMillis();
                System.out.println("Starting 15 second capture.");
            }

            if(System.currentTimeMillis() - startTime > duration){
                System.out.println("End of 15 second capture.");
                break;
            }
        }
        HighGui.destroyAllWindows();
    }

    public static double[] meanPixelValue(Mat frame){
        //plot twist opencv already does this for us wooooooo
        Scalar avg = Core.mean(frame);
        return new double[]{avg.val[0], avg.val[1], avg.val[2]};
    }
}
