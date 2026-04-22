package com.example.rppgkotlin

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.rppgkotlin.databinding.ActivityMainBinding
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.sqrt
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var faceLandmarker: FaceLandmarker
    private val signalBuffer = mutableListOf<Double>()
    private lateinit var roiDetector: ROIDetector
    private var currentFrame: Bitmap? = null
    private lateinit var liveSosFilter: LiveSosFilter
    private var isCapturing = true

    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService

    private val REQUEST_CODE_PERMISSIONS = 10
    private val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        cameraExecutor = Executors.newSingleThreadExecutor()
        viewBinding.bpmText.text = "BPM: --"
        setupFaceLandmarker()
        roiDetector = ROIDetector(faceLandmarker)
        val sos = arrayOf(
            doubleArrayOf(0.0004, 0.0008, 0.0004, 1.0, -1.79, 0.81),
            doubleArrayOf(1.0, 2.0, 1.0, 1.0, -1.89, 0.91)
        )
        liveSosFilter = LiveSosFilter(sos)
        if(allPermissionsGranted()){
            startCamera()
        }else{
            requestPermissions()
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onDestroy(){
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_CODE_PERMISSIONS){
            if(allPermissionsGranted()){
                startCamera()
            }else{
                requestPermissions()
            }
        }
    }

    private fun setupFaceLandmarker(){
        val baseOptions = BaseOptions.builder().setModelAssetPath("face_landmarker.task").build()
        val options = FaceLandmarker.FaceLandmarkerOptions.builder().setBaseOptions(baseOptions)
            .setRunningMode(com.google.mediapipe.tasks.vision.core.RunningMode.LIVE_STREAM)
            .setResultListener { result, _ ->
                //kotlin lambdas are so cool
                val frame = currentFrame ?: return@setResultListener

                if(result.faceLandmarks().isNotEmpty()){
                    //TODO: WHAT TO PUT HERE?
                    val mask = roiDetector.process(frame, result)
                    if(mask != null){
                        val avgGreen = extractGreenAverage(frame, mask)
                        val filteredGreen = liveSosFilter.process(avgGreen)
                        signalBuffer.add(filteredGreen)

                        if(signalBuffer.size >= 900){
                            isCapturing = false
                            val bpm = getBPMfromFFT(signalBuffer)

                            runOnUiThread {
                                viewBinding.bpmText.text = "FinalBPM : ${bpm.toInt()}"
                                stopCamera()
                            }
                        }
                        mask.recycle()
                    }
                }
            }
            .build()

        faceLandmarker = FaceLandmarker.createFromOptions(this, options)
        roiDetector = ROIDetector(faceLandmarker)
    }

    private fun extractGreenAverage(frame: Bitmap, mask: Bitmap): Double{
        var sumGreen = 0.0
        var count = 0
        val w = frame.width
        val h = frame.height

        val pixels = IntArray(w * h)
        val maskPixels = ByteArray(w * h)

        frame.getPixels(pixels, 0, w, 0, 0, w, h)

        val buffer = java.nio.ByteBuffer.allocate(mask.byteCount)
        mask.copyPixelsToBuffer(buffer)
        val maskArray = buffer.array()

        for(i in pixels.indices){
            if(maskArray[i].toInt() != 0){
                val green = Color.green(pixels[i])
                sumGreen += green
                count++
            }
        }

        if(count > 0){
            return sumGreen/count
        }else{
            return 0.0
        }
    }

    private fun getBPMfromFFT(signal: List<Double>, fs: Double = 30.0): Double{
        //this is such an interesting syntax of kt
        val processedSignal = if(signal.size > 900){
            signal.subList(60, 900).toDoubleArray()
        }else if(signal.size > 60){
            signal.subList(60, signal.size).toDoubleArray()
        }else{
            return 0.0
        }

        val nFFT = 2048
        val mean = processedSignal.average()

        //god kt is so cool
        //DC component removal
        val fftInput = DoubleArray(nFFT){ i ->
            if(i < processedSignal.size){
                processedSignal[i] - mean
            }else{
                0.0
            }
        }

        val fft = org.jtransforms.fft.DoubleFFT_1D(nFFT.toLong())
        fft.realForward(fftInput)

        val magnitudes = DoubleArray(nFFT / 2)
        for(i in 0 until nFFT/2){
            val re = fftInput[2 * i]
            val im = fftInput[2 * i +1]
            magnitudes[i] = sqrt(re * re + im * im)
        }

        var maxMag = -1.0
        var bestFreq = 0.0
        val lowerBound = 1.1
        val upperBound = 2.0
        //peak finding within [1.1, 2.0] hz range
        for(i in 0 until nFFT/2){
            val freq = i * fs /nFFT
            //THIS IS AN OPERATOR I CAN DO???
            if(freq in lowerBound..upperBound){
                if(magnitudes[i] > maxMag){
                    maxMag = magnitudes[i]
                    bestFreq = freq
                }
            }
        }

        return bestFreq * 60.0
    }

    private fun startCamera(){
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            //.also is SUCH a funny attribute name lol
            val preview = androidx.camera.core.Preview.Builder().build().also{
                it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                val bitmap = imageProxy.toBitmap()

                currentFrame = bitmap
                val mpImage = com.google.mediapipe.framework.image.BitmapImageBuilder(bitmap).build()
                faceLandmarker.detectAsync(mpImage, System.currentTimeMillis())

                imageProxy.close()
            }
            cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                imageAnalysis)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopCamera(){
        val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
        }, androidx.core.content.ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all{
        androidx.core.content.ContextCompat.checkSelfPermission(baseContext, it) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        androidx.core.app.ActivityCompat.requestPermissions(
            this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
        )
    }
}