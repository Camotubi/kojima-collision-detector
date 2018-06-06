package com.ecs.collisiondetector.EdgeDetection

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

class Canny {
    companion object {
        @JvmStatic
        fun detectEdges(bitmap: Bitmap): Bitmap {
            val rgba = Mat()
            Utils.bitmapToMat(bitmap, rgba)

            val edges = Mat(rgba.size(), CvType.CV_8UC1)
            Imgproc.cvtColor(rgba, edges, Imgproc.COLOR_RGB2GRAY, 4)
            Imgproc.Canny(edges, edges, 80.0, 100.0)
            val resBitmap = Bitmap.createBitmap(edges.cols(), edges.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(edges, resBitmap)
            return resBitmap

        }
    }

}