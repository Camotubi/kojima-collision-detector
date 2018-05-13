package com.ecs.collisiondetector

import android.content.Intent
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.view.WindowManager
import android.widget.Button
import com.ecs.collisiondetector.yolo2.view.ClassifierActivity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.widget.ImageView
import com.ecs.collisiondetector.EdgeDetection.Canny
import com.ecs.collisiondetector.EdgeDetection.EdgeDetector
import org.opencv.android.Utils
import org.opencv.imgproc.Imgproc
import org.opencv.core.CvType
import java.nio.file.Files.size
import org.opencv.core.Mat
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.android.BaseLoaderCallback
import java.nio.ByteBuffer


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
        setContentView(R.layout.testing_layout)

        /*setContentView(R.layout.activity_main)
        val button : Button = findViewById(R.id.button)
        button.setOnClickListener(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }
        val fragment = PreviewFragment.newInstance()
        replaceFragment(fragment)*/
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

        val bmp = BitmapFactory.decodeResource(resources,R.drawable.wot)
        val edgeBmp = Canny.detectEdges(bmp)
        val edgeWidth = EdgeDetector.getWidth(edgeBmp)
        Log.d("WIDTH APARATO", "Width: $edgeWidth")
        val edgeDrawable = BitmapDrawable(resources,edgeBmp)
        val boxDrawable = BitmapDrawable(resources,bmp)
        val imgView = findViewById<ImageView>(R.id.imageView2)
        imgView.setImageDrawable(edgeDrawable)
    }
    override fun onClick(v: View?) {
        val intent = Intent(this, ClassifierActivity::class.java)
        this.startActivity(intent)
    }

    //Function to call and manage the fragments
    private fun replaceFragment(fragment: android.support.v4.app.Fragment?){
       /* val fragmentTransaction = supportFragmentManager.beginTransaction()
        //To replace the fragment it needs the id of the container
        // in this case being the Frame Layout of the Activity
        // and the fragement that is going to be replaced with
        fragmentTransaction.replace(fragmentContainer.id, fragment)
        fragmentTransaction.commit()*/
    }


}
