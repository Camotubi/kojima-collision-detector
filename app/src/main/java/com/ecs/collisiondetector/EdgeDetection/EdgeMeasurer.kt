package com.ecs.collisiondetector.EdgeDetection

import android.graphics.Bitmap
import android.graphics.BitmapFactory

class EdgeMeasurer {
    var bitmap :Bitmap = Bitmap.createBitmap(1,1,Bitmap.Config.RGB_565)
    fun EdgeDetector(bmp: Bitmap)
    {
        bitmap = bmp
    }
    companion object {
        fun getFarthest(bmp : Bitmap) : Array<IntArray> {
            var p1 = intArrayOf(-1,-1)
            var p2 = intArrayOf(-1,-1)
            for(y in 0..(bmp.height-1)) {
                for (x in 0..(bmp.width-1)) {
                    val pix = bmp.getPixel(x,y)
                    if(pix != -16777216) {
                        if(p1[0]==-1) {
                            p1[0] = x
                            p1[1] = y
                        } else {
                            p2[0] = x
                            p2[1] = y
                        }
                    }
                }
            }
            return arrayOf(p1,p2)
        }
        @JvmStatic
        fun getWidth(bmp: Bitmap) : Int{
            val farthests = getFarthest(bmp)
            return Math.abs(farthests[0][0] - farthests[1][0])
        }
        fun getHeigth(bmp: Bitmap) : Int{
            val farthests = getFarthest(bmp)
            return Math.abs(farthests[0][1] - farthests[1][1])
        }
    }
}