package com.opencvapp

import android.graphics.Bitmap
import android.graphics.Color
import com.facebook.react.bridge.*
import java.util.Collections
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.sqrt

class OpenCVModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    internal val signalBuffer = Collections.synchronizedList(mutableListOf<Double>())
    internal var isMeasuring = false
    internal var frameCount = 0

    // Dependencies (Initialized in your project)
    internal lateinit var faceLandmarker: FaceLandmarker
    internal lateinit var roiDetector: ROIDetector
    internal lateinit var liveSosFilter: LiveSosFilter

    companion object {
        private var instance: OpenCVModule? = null
        fun getInstance(): OpenCVModule? = instance
    }

    init {
        instance = this
    }

    override fun getName(): String = "OpenCVModule"

    @ReactMethod
    fun startMeasurement(promise: Promise) {
        synchronized(signalBuffer) {
            signalBuffer.clear()
            frameCount = 0
            isMeasuring = true
        }
        promise.resolve(true)
    }

    @ReactMethod
    fun getFinalBPM(promise: Promise) {
        isMeasuring = false

        // Use a background thread so we don't freeze the UI during heavy math
        Thread {
            val data = synchronized(signalBuffer) { signalBuffer.toDoubleArray() }

            if (data.size < 300) {
                promise.reject("SIGNAL_TOO_SHORT", "Need at least 10 seconds of data.")
                return@Thread
            }

            try {
                // 1. Pre-process: Detrend & Hanning Window
                val mean = data.average()
                val processedSignal = DoubleArray(data.size)
                for (i in data.indices) {
                    val window = 0.5 * (1 - cos(2 * PI * i / (data.size - 1)))
                    processedSignal[i] = (data[i] - mean) * window
                }

                // 2. Execute the FFT logic you provided
                val bpm = performFFTAnalysis(processedSignal, 30.0)

                promise.resolve(bpm)
            } catch (e: Exception) {
                promise.reject("FFT_ERROR", e.message)
            }
        }.start()
    }


    private fun performFFTAnalysis(signal: DoubleArray, samplingRate: Double): Double {
        val n = signal.size
        var maxMagnitude = -1.0
        var peakFrequency = 0.0

        // We only care about frequencies in the human heart rate range
        // 42 BPM (0.7 Hz) to 180 BPM (3.0 Hz)
        val minFreq = 0.7
        val maxFreq = 3.0

        // Iterate through frequency bins
        for (f in 0 until n / 2) {
            val freq = f * samplingRate / n

            // Filter for heart rate range
            if (freq in minFreq..maxFreq) {
                var real = 0.0
                var imag = 0.0

                // Standard DFT summation
                for (t in 0 until n) {
                    val angle = 2.0 * Math.PI * f * t / n
                    real += signal[t] * Math.cos(angle)
                    imag -= signal[t] * Math.sin(angle)
                }

                val magnitude = Math.sqrt(real * real + imag * imag)

                if (magnitude > maxMagnitude) {
                    maxMagnitude = magnitude
                    peakFrequency = freq
                }
            }
        }

        // Convert frequency (Hz) to Beats Per Minute (BPM)
        return peakFrequency * 60.0
    }
    // Called by the Plugin
    internal fun processFrameData(greenValue: Double) {
        if (isMeasuring) {
            signalBuffer.add(greenValue)
            frameCount++
            if (frameCount >= 900) isMeasuring = false
        }
    }

    @ReactMethod
    fun getStatus(promise: Promise) {
        val map = Arguments.createMap().apply {
            putInt("frameCount", frameCount)
            putBoolean("isMeasuring", isMeasuring)
        }
        promise.resolve(map)
    }
}