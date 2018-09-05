package com.ecs.collisiondetector

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.View.OnClickListener
import android.util.Log
import android.view.View.VISIBLE
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.android.BaseLoaderCallback
import java.io.*
import android.widget.CompoundButton
import com.ecs.collisiondetector.yolo2.view.ClassifierActivity


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

        val cameraButton : Button = findViewById(R.id.cameraButton)
        cameraButton.setOnClickListener(this)
        logButton.setOnClickListener(View.OnClickListener {
            logTextView.setText(SeeLog())
        })
        val spinner : Spinner = findViewById(R.id.spinner)
        val supportedDevicesStringList = DistanceCalculator.supportedDevicesList.map{ it.name }.toTypedArray()
        var spinnerAdapter = ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, supportedDevicesStringList)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter

        logCheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
            if (logCheckBox.isChecked){
                SecTextBox.visibility = VISIBLE
                SecTextView.visibility = VISIBLE
            }
        }

        val wasLogSuccessful = intent.getBooleanExtra("wasLogSuccessful", false)
        Log.e("Hola", "llegue aqui")
        if (wasLogSuccessful){
            logButton.visibility = VISIBLE
            Toast.makeText(baseContext, "Log was successful", Toast.LENGTH_SHORT)
        }else{
            Toast.makeText(baseContext, "Log wasn't successful", Toast.LENGTH_SHORT)
        }

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
        if (logCheckBox.isChecked){
            intent.putExtra("amountOfSeconds", Integer.parseInt(SecTextBox.text.toString()))
        }
        this.startActivity(intent)
    }

    private fun SeeLog(): String {
        val `is`: InputStream
        var logText = StringBuilder()
        try {
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Log.txt")
            `is` = FileInputStream(file)
            val br = BufferedReader(InputStreamReader(`is`))
            logText.append(br.readLine())
            `is`.close()
            br.close()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("FILE", "File not found")
        }
        return logText.toString()
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
