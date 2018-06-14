package com.ecs.collisiondetector

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import com.ecs.collisiondetector.yolo2.view.ClassifierActivity
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.util.Log
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import com.ecs.collisiondetector.EdgeDetection.Canny
import com.ecs.collisiondetector.EdgeDetection.EdgeMeasurer
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.android.BaseLoaderCallback


class MainActivity : AppCompatActivity() , OnClickListener {
    private val mLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.i("MainActivity", "OpenCV loaded successfully")
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.testing_layout)

        setContentView(R.layout.activity_main)

        val button : Button = findViewById(R.id.button)
        button.setOnClickListener(this)
        val spinner : Spinner = findViewById(R.id.spinner)
        val supportedDevicesStringList = DistanceCalculator.supportedDevicesList.map{ it.name }.toTypedArray()
        var spinnerAdapter = ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, supportedDevicesStringList)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter



    }

    public override fun onResume() {
        
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);

        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

    }
    override fun onClick(v: View?) {

        val intent = Intent(this, ClassifierActivity::class.java)
        val spinner = findViewById<Spinner>(R.id.spinner)
        val focalLength =  DistanceCalculator.supportedDevicesList[spinner.selectedItemPosition].focalLength
        intent.putExtra("focalLength",focalLength)
        this.startActivity(intent)
    }

    //Function to call and manage the fragments
    private fun replaceFragment(fragment: android.support.v4.app.Fragment?){
      /* val fragmentTransaction = supportFragmentManager.beginTransaction()
        //To replace the fragment it needs the id of the container
        // in this case being the Frame Layout of the Activity
        // and the fragement that is going to be replaced with
        fragmentTransaction.replace(fragmentContainer.id, fragment)
        fragmentTransaction.commit()
        */
    }


}
