package com.ecs.collisiondetector.Models

import com.ecs.collisiondetector.yolo2.model.BoxPosition

data class Recognition (val id: Integer, val title: String, val confidence : Double, val Location :BoxPosition)