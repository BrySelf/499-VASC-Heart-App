import org.opencv.core.*;
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
        String videoPath = "testVideos/testvideo01_BPM_90.mp4";
        long startTime = System.currentTimeMillis();
        VideoCapture camera = new VideoCapture(videoPath);

        double fps = camera.get(Videoio.CAP_PROP_FPS);

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

        fifteenCapture(camera, frame, values, detector);

        camera.release();

        System.out.println("Pre-BPF size: " + values.size());

        ArrayList<Double> cleanData = bandPassFilter(values);
//        for (double[] val : cleanData) {
//            System.out.println(val);
//        }

        double heartRate = calculateBPM(cleanData, fps);
        System.out.println("Heart Rate:" + heartRate);

        System.out.println("Elapsed Time: " + (System.currentTimeMillis() - startTime));

        //annoyingly i *think* bc OpenCV is just c++ theres some issue with not
        //properly terminating the process once the end of the main method is reached
        //so i just solved it by force exiting.
        System.exit(0);
    }
    public static void fifteenCapture(VideoCapture camera, Mat frame, ArrayList<double[]> values, CascadeClassifier detector){
        while (camera.read(frame)) {
            if (!frame.empty()) {
                MatOfRect faces = new MatOfRect();
                detector.detectMultiScale(frame, faces); // Only detect every 5th frame
                //detection loop
                for (Rect face : faces.toArray()) {
                    //defining our face rectangle
                    Imgproc.rectangle(frame, face, new Scalar(255, 255, 255), 1);

                    Rect forehead = new Rect(
                            face.x + (int) (face.width * 0.3),
                            face.y + (int) (face.height * 0.08),
                            (int) (face.width * 0.4),
                            (int) (face.height * 0.15)
                    );

                    Rect leftCheek = new Rect(
                            face.x + (int) (face.width * 0.15),
                            face.y + (int) (face.height * 0.55),
                            (int) (face.width * 0.2),
                            (int) (face.height * 0.2)
                    );

                    Rect rightCheek = new Rect(
                            face.x + (int) (face.width * 0.65),
                            face.y + (int) (face.height * 0.55),
                            (int) (face.width * 0.2),
                            (int) (face.height * 0.2)
                    );

                    //draw rects
                    Imgproc.rectangle(frame, forehead, new Scalar(0, 255, 0), 1);
                    Imgproc.rectangle(frame, leftCheek, new Scalar(0, 255, 0), 1);
                    Imgproc.rectangle(frame, rightCheek, new Scalar(0, 255, 0), 1);

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
                    double redMPV = (foreheadMPV[2] + leftCheekMPV[2] + rightCheekMPV[2])/3.0;
                    double greenMPV = (foreheadMPV[1] + leftCheekMPV[1] + rightCheekMPV[1])/3.0;
                    double blueMPV = (foreheadMPV[0] + leftCheekMPV[0] + rightCheekMPV[0])/3.0;
                    values.add(new double[]{blueMPV, greenMPV, redMPV});
                }
            }
        }
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
            double[] fastAvg = getChannelAverages(signalData, i, lowPassWindow);
            double[] slowAvg = getChannelAverages(signalData, i, highPassWindow);

            double greenAC = fastAvg[1] - slowAvg[1];
            double redAC   = fastAvg[2] - slowAvg[2];

            //green is really pulse heavy and blue and red are really noise heavy
            //so we do Signal = 3*G - 2*R
            double combinedPulse = (3.0 * greenAC) - (2.0 * redAC);

            filteredSignal.add(combinedPulse); //centers around 0
        }
        return filteredSignal;
    }

    private static double[] getChannelAverages(ArrayList<double[]> data, int index, int window) {
        double blueSum = 0, greenSum = 0, redSum = 0;
        for (int j = 0; j < window; j++) {
            double[] frame = data.get(index - j);
            blueSum += frame[0];
            greenSum += frame[1];
            redSum += frame[2];
        }
        return new double[]{blueSum / window, greenSum / window, redSum / window};
    }

    public static double calculateBPM(ArrayList<Double> signal, double fps){
        //calculate actual duration, we lose some frames due to BPF
        System.out.println("Signal Size: " + signal.size());

        int dftSize = 2048;

        //even though our values have been 8-bit, we need 32-bit room for the math
        Mat signalMat = new Mat(dftSize, 1, CvType.CV_32F, new Scalar(0));


        //mean normalizing
        double mean = 0;
        for(double d : signal) mean += d;
        mean /= signal.size();
        float[] floatSignal = new float[signal.size()];
        for(int i = 0; i < floatSignal.length; i++){
            floatSignal[i] = (float)(signal.get(i) - mean);
        }

        //hamming window!!!
        for (int i = 0; i < signal.size(); i++) {
            //hamming Window formula
            double multiplier = 0.54 - 0.46 * Math.cos(2 * Math.PI * i / (signal.size() - 1));
            floatSignal[i] = (float) (floatSignal[i] * multiplier);
        }

        signalMat.put(0, 0, floatSignal);
        for(int i = 0; i < floatSignal.length; i++){
            floatSignal[i] = signal.get(i).floatValue();
        }
        signalMat.put(0,0, floatSignal);
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
            double frequency = (double) i * fps / dftSize;
            if(frequency >= 0.96 && frequency <= 1.9){
                double val = magnitude.get(i, 0)[0];
                System.out.println(i + "," + frequency + "," + val);
                if(val > maxVal){
                    maxVal = val;
                    maxIdx = i;
                }
            }
        }

        System.out.println("maxIdx: " + maxIdx);

        signalMat.release(); //manually releasing for safety
        dftMat.release();
        magnitude.release();


        return ((maxIdx * fps) / dftSize) * 60;
    }
}

