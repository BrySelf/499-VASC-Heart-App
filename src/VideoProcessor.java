import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

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
        //later this will be a stored video taken directly from the
        //camera, not a video we have already taken.
        VideoCapture videoCap = new VideoCapture("placeholder/path.mp4");
        Mat frame = new Mat();

        if(!videoCap.isOpened()){
            System.err.println("Could not open Video.");
            return;
        }

        int noRows = frame.rows();
        int noCols = frame.cols();
        int noChannels = frame.channels(); //should be 3 for us
        /*
        * i love how every docstring for functions is just whatever c++ function
        * it's running, parameters included. like literally a line of code.
        * for Mat.channels() it says "int Mat::channels()" its like oh ok yeah i
        * didn't know that's what it did.
        */
        /*
        * or alternatively if you go to the javadocs it my say something like this:
        * "public int get(int row, int col, byte[] data)"
        * and its like yeah OK that's the method signature cool but what does it DO??
        * HOW does it do it?? are there any other things i should know about??
         */

        /*
        * kind of annoyingly we *have* to use a byte array here. we're essentially
        * trying to mimic how c++ arrays work. if we don't we're asking for every
        * pixel of every frame to have get() called on, which opencv will then perform
        * transformations on in order to get it into a double[]. this is because Mat
        * stores its data as raw bytes in off-heap memory. in addition, its more
        * memory efficient. the double[] we would store in is 24 bytes per pixel
        * for the same data we could store in a byte[] for 3 bytes a pixel. we do
        * have to handle the fact that java bytes are signed by default but that just
        * requires some bitwise operands and unsigned ints
        */
        byte[] data = new byte[noRows * noCols * noCols];
        frame.get(0, 0, data);

        for(int i = 0; i < data.length; i+= 3){

            /*
            * 330 review:
            * lets say we have unsigned byte x = 1000 0001, or in decimal terms, 129. but
            * java interprets all bytes as signed by default (specifically using
            * two's complement, go look back at your notes for more info if you need)
            * so java would evaluate x = 1000 0001 as -127 in decimal.
            *
            * to remedy this we use bitwise AND (&) and converting to an integer.
            * recall that & returns 1 for given bit if both input bits are 1, 0 otherwise.
            * by calling & with 0xFF as our other input, we can handle the
            * signed/unsigned gap between c++ and java.
            *
            * e.g.
            * starting off lets say data[i] is 1000 0001.
            * we assign it to int x. java performs sign extension when turning it into
            * a 32-bit int, by filling everything to the left with the sign bit.
            * so:
            * x = 11111111 11111111 11111111 10000001
            * 0xFF = 00000000 00000000 00000000 11111111
            * x & 0xFF = 00000000 00000000 00000000 10000001
            *
            * now when reading x & 0xFF as an int, java will correctly understand its
            * 129 and not -127. obviously we need this because BGR values are [0, 255]
            * and not [-128, 127] like a two's complement byte.
             */
            int b = data[i] & 0xFF; //handle signed bytes
            int g = data[i+1] & 0xFF;
            int r = data[i+2] & 0xFF;
        }
    }
}
