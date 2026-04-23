package com.opencvapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.facebook.react.bridge.*

class OpenCVModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    private val processor = HeartRateProcessor(reactContext)

    override fun getName(): String = "OpenCVModule"

    @ReactMethod
    fun resetMeasurement(promise: Promise) {
        try {
            processor.reset()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("RESET_ERROR", e)
        }
    }

    @ReactMethod
    fun processFrame(base64Image: String, timestampMs: Double, fps: Double, promise: Promise) {
        try {
            val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            if (bitmap == null) {
                promise.reject("BITMAP_ERROR", "Could not decode bitmap")
                return
            }

            val bpm = processor.processBitmap(bitmap, timestampMs.toLong(), fps)

            if (bpm == null) {
                promise.resolve(null)
            } else {
                promise.resolve(bpm)
            }
        } catch (e: Exception) {
            promise.reject("PROCESS_FRAME_ERROR", e.message, e)
        }
    }
}