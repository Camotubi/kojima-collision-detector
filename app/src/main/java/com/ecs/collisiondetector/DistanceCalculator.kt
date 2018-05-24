package com.ecs.collisiondetector
class DistanceCalculator {

    var focalLength:Double = 0.0


  fun calibrateFocalLength(pixelWidth:Double, width:Double, distance:Double) {
    this.focalLength = calculateFocalLength(pixelWidth, width, distance)
  }

  fun calculateDistance(pixelWidth:Double, width:Double):Double {
    return width * this.focalLength / pixelWidth
  }

  companion object {
    fun calculateFocalLength(pixelWidth:Double, width:Double, distance:Double):Double {
      return pixelWidth * distance / width
    }
    @JvmStatic
    fun calculateDistance(pixelWidth:Double, width:Double, focalLength:Double):Double {
      return width * focalLength / pixelWidth
    }
  }
  
}
