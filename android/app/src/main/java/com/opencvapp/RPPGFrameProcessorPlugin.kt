package com.opencvapp

import android.graphics.*
import android.media.Image
import com.mrousavy.camera.frameprocessors.Frame
import com.mrousavy.camera.frameprocessors.FrameProcessorPlugin
import com.mrousavy.camera.frameprocessors.VisionCameraProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class RPPGFrameProcessorPlugin(proxy: VisionCameraProxy, options: Map<String, Any>?) : FrameProcessorPlugin() {

    override fun callback(frame: Frame, params: Map<String, Any>?): Any? {
        val module = OpenCVModule.getInstance() ?: return null
        if (!module.isMeasuring) return null

        try {
            // 1. Convert Frame to Bitmap
            val bitmap = toBitmap(frame) ?: return null

            // 2. Run Face Detection
            val image = BitmapImageBuilder(bitmap).build()
            val detectionResult = module.faceLandmarker.detect(image)

            if (detectionResult.faceLandmarks().isNotEmpty()) {
                // 3. Get ROI Mask (Forehead/Cheeks)
                val mask = module.roiDetector.process(bitmap, detectionResult)

                if (mask != null) {
                    // 4. Extract Green Average
                    val rawGreen = module.extractGreenAverage(bitmap, mask)

                    // 5. Apply SOS Filter (Clean the pulse signal)
                    val filteredGreen = module.liveSosFilter.process(rawGreen)

                    // 6. Push to Buffer
                    module.processFrameData(filteredGreen)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("RPPGPlugin", "Processing error: ${e.message}")
        }

        return null
    }

    /**
     * Converts a Vision Camera Frame (YUV_420_888) to an Android Bitmap.
     * Handles rotation and format conversion.
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