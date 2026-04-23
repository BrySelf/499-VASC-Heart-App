package com.opencvapp

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import org.jtransforms.fft.DoubleFFT_1D
import kotlin.math.abs
import android.util.Log

class HeartRateProcessor(private val context: Context) {

    private var faceLandmarker: FaceLandmarker? = null
    private var roiDetector: ROIDetector? = null
    private var liveSosFilter: LiveSosFilter? = null

    private val rawGreenBuffer = mutableListOf<Double>()
    private val filteredGreenBuffer = mutableListOf<Double>()

    private var fpsEstimate: Double = 30.0

    companion object {
        private const val MODEL_NAME = "face_landmarker.task"
        private const val MIN_SECONDS_FOR_ESTIMATE = 3
        private const val DEFAULT_FPS = 30.0
        private const val MIN_BPM = 45.0
        private const val MAX_BPM = 180.0
    }

fun initialize() {
    if (faceLandmarker != null) return

    try {
        Log.d("HeartRateProcessor", "Loading model: $MODEL_NAME")

        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(MODEL_NAME)
            .build()

        val options = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.IMAGE)
            .setNumFaces(1)
            .build()

        faceLandmarker = FaceLandmarker.createFromOptions(context, options)
        Log.d("HeartRateProcessor", "FaceLandmarker created")

        roiDetector = ROIDetector(faceLandmarker!!)
        liveSosFilter = LiveSosFilter()
        Log.d("HeartRateProcessor", "Processor initialized")
    } catch (e: Exception) {
        Log.e("HeartRateProcessor", "initialize failed: ${e.message}", e)
        throw RuntimeException("initialize failed: ${e.message}", e)
    }
}

    fun reset() {
        rawGreenBuffer.clear()
        filteredGreenBuffer.clear()
        liveSosFilter?.reset()
    }

    fun close() {
        faceLandmarker?.close()
        faceLandmarker = null
        roiDetector = null
        liveSosFilter = null
        reset()
    }

    /**
     * Processes one frame.
     *
     * @param bitmap camera frame as Bitmap
     * @param timestampMs monotonically increasing frame timestamp
     * @param fps optional measured fps from camera pipeline
     * @return BPM if enough valid data exists, otherwise null
     */
    fun processBitmap(
        bitmap: Bitmap,
        timestampMs: Long,
        fps: Double? = null
    ): Double? {
        try {
            initialize()

            if (fps != null && fps > 0.0) {
                fpsEstimate = fps
            }

            val landmarker = faceLandmarker ?: return null
            val roi = roiDetector ?: return null
            val filter = liveSosFilter ?: return null

            android.util.Log.d(
                "HeartRateProcessor",
                "Processing bitmap ${bitmap.width}x${bitmap.height}, timestamp=$timestampMs"
            )

            val mpImage = BitmapImageBuilder(bitmap).build()

            val result: FaceLandmarkerResult = try {
                android.util.Log.d("HeartRateProcessor", "Calling detect()")
                landmarker.detect(mpImage)
            } catch (e: Exception) {
                android.util.Log.e("HeartRateProcessor", "detect failed: ${e.message}", e)
                throw RuntimeException("detect failed: ${e.message}", e)
            }

            android.util.Log.d(
                "HeartRateProcessor",
                "Faces found: ${result.faceLandmarks().size}"
            )

            if (result.faceLandmarks().isEmpty()) {
                return null
            }

            val mask = roi.process(bitmap, result)
            if (mask == null) {
                android.util.Log.d("HeartRateProcessor", "ROI mask was null")
                return null
            }

            val greenAverage = extractGreenAverage(bitmap, mask)
            if (greenAverage == null) {
                android.util.Log.d("HeartRateProcessor", "Green average was null")
                return null
            }

            rawGreenBuffer.add(greenAverage)

            val filteredValue = filter.process(greenAverage)
            filteredGreenBuffer.add(filteredValue)

            android.util.Log.d(
                "HeartRateProcessor",
                "Samples collected: ${filteredGreenBuffer.size}"
            )
            return if (hasEnoughData()) {
                val bpm = estimateBpm(filteredGreenBuffer, fpsEstimate)
                android.util.Log.d("HeartRateProcessor", "Estimated BPM: $bpm")
                bpm
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("HeartRateProcessor", "processBitmap failed: ${e.message}", e)
            throw RuntimeException("processBitmap failed: ${e.message}", e)
        }
    }

    private fun hasEnoughData(): Boolean {
        val requiredSamples = (fpsEstimate * MIN_SECONDS_FOR_ESTIMATE).toInt()
        return filteredGreenBuffer.size >= requiredSamples
    }

    private fun extractGreenAverage(bitmap: Bitmap, mask: Bitmap): Double? {
        val width = bitmap.width
        val height = bitmap.height

        val pixels = IntArray(width * height)
        val maskPixels = IntArray(width * height)

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        mask.getPixels(maskPixels, 0, width, 0, 0, width, height)

        var greenSum = 0.0
        var count = 0

        for (i in pixels.indices) {
            // ALPHA_8 mask: non-zero means included ROI
            if (maskPixels[i] != 0) {
                val pixel = pixels[i]
                val green = (pixel shr 8) and 0xFF
                greenSum += green
                count++
            }
        }

        if (count == 0) return null
        return greenSum / count
    }

    private fun estimateBpm(signal: List<Double>, fps: Double): Double? {
        if (signal.size < 2) return null

        val n = signal.size
        val mean = signal.average()
        val fftInput = DoubleArray(n * 2)

        for (i in 0 until n) {
            fftInput[2 * i] = signal[i] - mean
            fftInput[2 * i + 1] = 0.0
        }

        val fft = DoubleFFT_1D(n.toLong())
        fft.complexForward(fftInput)

        var bestFreq = 0.0
        var bestMagnitude = 0.0

        for (k in 1 until n / 2) {
            val freqHz = k * fps / n
            val bpm = freqHz * 60.0

            if (bpm in MIN_BPM..MAX_BPM) {
                val re = fftInput[2 * k]
                val im = fftInput[2 * k + 1]
                val magnitude = abs(re * re + im * im)

                if (magnitude > bestMagnitude) {
                    bestMagnitude = magnitude
                    bestFreq = freqHz
                }
            }
        }

        return if (bestFreq > 0.0) bestFreq * 60.0 else null
    }
}