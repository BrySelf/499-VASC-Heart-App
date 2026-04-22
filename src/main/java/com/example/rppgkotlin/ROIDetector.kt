package com.example.rppgkotlin

import android.graphics.*
import com.google.mediapipe.tasks.vision.facelandmarker.*
import androidx.core.graphics.createBitmap

class ROIDetector(private val faceLandmarker: FaceLandmarker){
    private val lowerFaceIndices = intArrayOf(200, 431, 411, 340, 349, 120, 111, 187, 211)

    fun process(bitmap: Bitmap, results: FaceLandmarkerResult): Bitmap?{
        if (results.faceLandmarks().isEmpty()) return null

        val h = bitmap.height
        val w = bitmap.width
        val landmarks = results.faceLandmarks()[0]

        //create point list
        val path = Path()
        for(i in lowerFaceIndices.indices){
            val landmark = landmarks[lowerFaceIndices[i]]
            val x = landmark.x() * w
            val y = landmark.y() * h

            if(i == 0){
                path.moveTo(x, y)
            }else{
                path.lineTo(x, y)
            }
        }
        path.close()

        //mask creation
        val mask = createBitmap(w, h, Bitmap.Config.ALPHA_8)
        val canvas = Canvas(mask)
        val paint = Paint().apply{
            color = Color.BLACK //ALPHA_8, so all colors are fill
            style = Paint.Style.FILL
            isAntiAlias = false
        }
        canvas.drawPath(path, paint)

        return mask
    }
}