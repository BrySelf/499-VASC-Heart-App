package com.opencvapp

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import com.facebook.react.bridge.Arguments
import com.mrousavy.camera.frameprocessors.Frame
import com.mrousavy.camera.frameprocessors.FrameProcessorPlugin
import com.mrousavy.camera.frameprocessors.VisionCameraProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder

class RPPGFrameProcessorPlugin(proxy: VisionCameraProxy, options: Map<String, Any>?) : FrameProcessorPlugin() {

    override fun callback(frame: Frame, params: Map<String, Any>?): Any? {
        // 1. Get our Module instance to access the shared buffer and state
        val module = OpenCVModule.getInstance() ?: return null

        // 2. Only process if the user has toggled "Start" in the React Native UI
        if (!module.isMeasuring) return null

        try {
            // 3. Convert the Camera Frame to a Bitmap for OpenCV/MediaPipe processing
            // Vision Camera frames are usually rotated; we need to fix orientation
            val bitmap = frame.toBitmap() ?: return null

            // 4. Run Face Detection (MediaPipe)
            val image = BitmapImageBuilder(bitmap).build()
            val detectionResult = module.faceLandmarker.detect(image)

            if (detectionResult.faceLandmarks().isNotEmpty()) {
                // 5. Detect ROI (Forehead/Cheeks) using your ROIDetector
                val mask = module.roiDetector.process(bitmap, detectionResult)

                if (mask != null) {
                    // 6. Extract the Average Green value from the ROI
                    val rawGreen = module.extractGreenAverage(bitmap, mask)

                    // 7. Apply your SOS Filter to clean the signal in real-time
                    val filteredGreen = module.liveSosFilter.process(rawGreen)

                    // 8. Push the clean data point into the 900-frame buffer
                    module.processFrameData(filteredGreen)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("RPPGPlugin", "Frame processing failed: ${e.message}")
        }

        return null // We return null because JS doesn't need this data frame-by-frame
    }

    /**
     * Extension helper to convert Vision Camera Frame to Android Bitmap
     */
    private fun toBitmap(frame: Frame): Bitmap? {
        val image: Image = frame.image ?: return null

        // 1. Convert YUV to JPEG bytes (Standard Android approach for stability)
        val yBuffer: ByteBuffer = image.planes[0].buffer
        val uBuffer: ByteBuffer = image.planes[1].buffer
        val vBuffer: ByteBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)
        val imageBytes = out.toByteArray()

        val rawBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        // 2. Fix Rotation (Front cameras are often 270 degrees)
        return if (frame.orientation != "portrait") {
            val matrix = Matrix()
            // Map Vision Camera orientation strings to degrees
            val degrees = when (frame.orientation) {
                "portrait" -> 0f
                "landscape-right" -> 90f
                "portrait-upside-down" -> 180f
                "landscape-left" -> 270f
                else -> 0f
            }
            matrix.postRotate(degrees)

            // For front camera, we usually need to mirror it as well
            matrix.postScale(-1f, 1f, rawBitmap.width / 2f, rawBitmap.height / 2f)

            Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
        } else {
            rawBitmap
        }
    }
}