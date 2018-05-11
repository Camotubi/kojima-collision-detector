package com.ecs.collisiondetector

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import com.ecs.collisiondetector.yolo2.view.ClassifierActivity

class MainActivity : AppCompatActivity() , OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val button : Button = findViewById(R.id.button)
        button.setOnClickListener(this)

    }

    override fun onClick(v: View?) {
        val intent = Intent(this, ClassifierActivity::class.java)
        this.startActivity(intent)
    }
}
