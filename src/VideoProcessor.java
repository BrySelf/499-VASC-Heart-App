import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
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
        long startTime = System.currentTimeMillis();

        fifteenCapture(camera, frame, values, detector);

        camera.release();

        ArrayList<Double> cleanData = bandPassFilter(values);
        for (Double val : cleanData) {
            System.out.println(val);
        }

        calculateBPM()

        //annoyingly i *think* bc OpenCV is just c++ theres some issue with not
        //properly terminating the process once the end of the main method is reached
        //so i just solved it by force exiting.
        System.exit(0);
    }

    public static void fifteenCapture(VideoCapture camera, Mat frame, ArrayList<double[]> values, CascadeClassifier detector){
        long startTime = 0;
        long duration = 15000; //in ms

        while (true) {
            if (camera.read(frame) && !frame.empty()) {
                MatOfRect faces = new MatOfRect();
                detector.detectMultiScale(frame, faces);
                //detection loop
                for(Rect face : faces.toArray()){
                    //defining our face rectangle
                    Imgproc.rectangle(frame, face, new Scalar(255,255,255),1);

                    Rect forehead = new Rect(
                            face.x + (int)(face.width*0.3),
                            face.y + (int)(face.height*0.08),
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
                    Imgproc.rectangle(frame, forehead, new Scalar(0,255,0),1);
                    Imgproc.rectangle(frame, leftCheek, new Scalar(0,255,0),1);
                    Imgproc.rectangle(frame, rightCheek, new Scalar(0,255,0),1);

                    //read and store values
                    Mat foreheadData = new Mat(frame, forehead);
                    Mat leftCheekData = new Mat(frame, leftCheek);
                    Mat rightCheekData = new Mat(frame, rightCheek);

                    double[] foreheadMPV = meanPixelValue(foreheadData);
                    double[] leftCheekMPV = meanPixelValue(leftCheekData);
                    double[] rightCheekMPV = meanPixelValue(rightCheekData);

                    foreheadData.release();
                    leftCheekData.release();
                    rightCheekData.release();

                    long elapsed = System.currentTimeMillis() - startTime;
                    double[] dataArray = new double[]{
                            (double)elapsed,
                            argMax(new double[]{
                                    foreheadMPV[1], leftCheekMPV[1], rightCheekMPV[1]
                            })
                    };
                    values.add(dataArray);

//                    System.out.printf("Greens = [%f, %f, %f]%n",
//                            dataArray[1], dataArray[2], dataArray[3]);
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

    //simplified BandPass
    public static ArrayList<Double> bandPassFilter(ArrayList<double[]> signalData){
        ArrayList<Double> filteredSignal = new ArrayList<>();

        /*
        * so we need to block the super jittery fast signal (low pass) AND the super-slow
        * drift (high-pass). that gives us the important stuff in the middle
        */
        int lowPassWindow = 5; //fps
        int highPassWindow = 45; //fps

        for(int i=highPassWindow; i < signalData.size(); i++){
            double smallSum = 0;
            for(int j=0; j < lowPassWindow; j++){
                smallSum += signalData.get(i-j)[1];//grabbing average of last 5 frames
            }
            double fastAvg = smallSum / lowPassWindow;

            double largeSum = 0;
            for(int j = 0; j < highPassWindow; j++){
                largeSum += signalData.get(i-j)[1];//avg of last 45 frames
            }
            double slowAvg = largeSum / highPassWindow;

            //the actual filtering step
            filteredSignal.add(fastAvg - slowAvg); //centers around 0
        }
        return filteredSignal;
    }

    //needed a max function
    public static double argMax(double[] args){
        double max = args[1];
        for(int i = 1; i < args.length; i++){
            if(args[i] > max){
                max = args[i];
            }
        }
        return max;
    }

    public static double calculateBPM(ArrayList<double[]> signal, double fps){
        int dftSize = Core.getOptimalDFTSize(signal.size());

        //even though our values have been 8-bit, we need 32-bit room for the math
        Mat signalMat = new Mat(dftSize, 1, CvType.CV_32F, new Scalar(0));

        Mat dftMat = new Mat();
        //in order to accurately get the result, we need to return it as a complex number
        Core.dft(signalMat, dftMat, Core.DFT_COMPLEX_OUTPUT);

        //finding magnitude of each output
        ArrayList<Mat> channels = new ArrayList<>();
        Core.split(dftMat, channels);
        Mat magnitude = new Mat();
        Core.magnitude(channels.get(0), channels.get(1), magnitude);

        double maxVal = 0;
        double maxIdx = -1;

        for(int i = 1; i < dftSize / 2; i++){
            double frequency = (double) i * fps/dftSize;
            if(frequency >= 0.7 && frequency <= 4.1){
                double val = magnitude.get(i, 0)[0];
                if(val > maxVal){
                    maxVal = val;
                    maxIdx = i;
                }
            }
        }

        signalMat.release(); //manually releasing for safety
        dftMat.release();
        magnitude.release();


        return maxIdx * fps / dftSize;
    }
}
