package com.ecs.collisiondetector

import android.provider.ContactsContract

class DistanceCalculator {

    var focalLength:Double = 0.0
  constructor(focalLength: Double) {
    this.focalLength = focalLength
  }

  fun calibrateFocalLength(pixelWidth:Double, width:Double, distance:Double) {
    this.focalLength = calculateFocalLength(pixelWidth, width, distance)
  }

  fun calculateDistance(pixelWidth:Double, width:Double):Double {
    return width * this.focalLength / pixelWidth
  }

  companion object {

      @JvmStatic val supportedDevicesList : List<PhoneDevice> = listOf(
              PhoneDevice("Honor 6x",26.0),
              PhoneDevice("One Plus 2", 38.462),
              PhoneDevice("One Plus 5T", 27.0)
      )
      @JvmStatic
    fun calculateFocalLength(pixelWidth:Double, width:Double, distance:Double):Double {
      return pixelWidth * distance / width
    }
    @JvmStatic
    fun calculateDistance(pixelWidth:Double, width:Double, focalLength:Double):Double {
      return width * focalLength / pixelWidth
    }
  }
  
}
