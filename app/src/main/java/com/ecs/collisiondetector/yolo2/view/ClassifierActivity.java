package com.ecs.collisiondetector.yolo2.view;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;

import com.ecs.collisiondetector.DistanceCalculator;
import com.ecs.collisiondetector.EdgeDetection.Canny;
import com.ecs.collisiondetector.EdgeDetection.EdgeMeasurer;
import com.ecs.collisiondetector.MainActivity;
import com.ecs.collisiondetector.yolo2.TensorFlowImageRecognizer;
import com.ecs.collisiondetector.yolo2.model.BoxPosition;
import com.ecs.collisiondetector.yolo2.model.Recognition;
import com.ecs.collisiondetector.yolo2.util.ImageUtils;

import com.ecs.collisiondetector.R;

import com.ecs.collisiondetector.yolo2.view.components.BorderedText;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import static com.ecs.collisiondetector.yolo2.Config.INPUT_SIZE;
import static com.ecs.collisiondetector.yolo2.Config.LOGGING_TAG;

/**
 * Classifier activity class
 * Modified by Zoltan Szabo
*/
public class ClassifierActivity extends TextToSpeechActivity implements OnImageAvailableListener {
    private boolean MAINTAIN_ASPECT = true;
    private float TEXT_SIZE_DIP = 10;

    private TensorFlowImageRecognizer recognizer;
    private Integer sensorOrientation;
    private int previewWidth = 0;
    private int previewHeight = 0;
    private Bitmap croppedBitmap = null;
    private boolean computing = false;
    private Matrix frameToCropTransform;
    private Matrix     imageFrameToCropTransform;

    private OverlayView overlayView;
    private BorderedText borderedText;
    private long lastProcessingTimeMs;
    private long lastDistanceProcessingTimeMs = 0;
    private Double focalLength = 0.0;
    private int widthInPix = 0;
    private Bitmap imageBitmap = null;
    private long timeToDetectCar = 0;
    private long lastTimeDetectcar = 0;
    private long timeToCompleteLog = 0;
    private long startActivityTime = 0;
    private double lastDistance = 0;
    private boolean logSuccessful = false;
    private long currentThreadTime = 0;



    private  List<Recognition> results;
    @Override
    public synchronized  void onCreate(Bundle bundle) {
        Intent intent = getIntent();
        focalLength = intent.getDoubleExtra("focalLength", 0.0);
        timeToCompleteLog = intent.getIntExtra("amountOfSeconds", 0) * 1000;
        startActivityTime = SystemClock.uptimeMillis();
        super.onCreate(bundle);
    }
    @Override
    public synchronized void onResume() {

        super.onResume();
    }

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        recognizer = TensorFlowImageRecognizer.create(getAssets());

        overlayView = (OverlayView) findViewById(R.id.overlay);
        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        final int screenOrientation = getWindowManager().getDefaultDisplay().getRotation();

        Log.i(LOGGING_TAG, String.format("Sensor orientation: %d, Screen orientation: %d",
                rotation, screenOrientation));

        sensorOrientation = rotation + screenOrientation;

        Log.i(LOGGING_TAG, String.format("Initializing at size %dx%d", previewWidth, previewHeight));

        croppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Config.ARGB_8888);

        imageBitmap = Bitmap.createBitmap(640,640,Config.ARGB_8888);
        imageFrameToCropTransform = ImageUtils.getTransformationMatrix(previewWidth, previewHeight,
                640, 640, sensorOrientation, MAINTAIN_ASPECT);
        imageFrameToCropTransform.invert(new Matrix());

        frameToCropTransform = ImageUtils.getTransformationMatrix(previewWidth, previewHeight,
                INPUT_SIZE, INPUT_SIZE, sensorOrientation, MAINTAIN_ASPECT);
        frameToCropTransform.invert(new Matrix());

        addCallback((final Canvas canvas) -> renderAdditionalInformation(canvas));


    }

    @Override
    public void onImageAvailable(final ImageReader reader) {
        Image image = null;


        try {
            image = reader.acquireLatestImage();

            if (image == null) {
                return;
            }

            if (computing) {
                image.close();
                return;
            }

            computing = true;
            fillCroppedBitmap(image);

            image.close();
        } catch (final Exception ex) {
            if (image != null) {
                image.close();
            }
            Log.e(LOGGING_TAG, ex.getMessage());
        }
        if(results == null) {
            lastTimeDetectcar = SystemClock.uptimeMillis();
        } else {
            for(int i = 0 ; i < results.size() ; i++){
                if(results.get(i).getTitle().equals("car") && timeToDetectCar ==0) {
                    timeToDetectCar = SystemClock.uptimeMillis() - lastTimeDetectcar;
                    textToWrite += "Detected car in: " + timeToDetectCar;
                }
            }
        }
        runInBackground(() -> {
            Log.e(LOGGING_TAG,"MELLAMAN");
            final long startTime = SystemClock.uptimeMillis();
            results = recognizer.recognizeImage(croppedBitmap);
            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
            overlayView.setResults(results);
            requestRender();
            computing = false;
        });
        runInBackground2(() -> {
            Log.e("Thread2", "Ya entre!!!");
            currentThreadTime = SystemClock.uptimeMillis();
               if (results != null && results.size() > 0) {
                   final long startTime = SystemClock.uptimeMillis();
                   BoxPosition yoloBox = results.get(0).getLocation();
                   RectF yoloRect = overlayView.reCalcSize(yoloBox);


                    /*if (
                            yoloBox.getLeft() > 0 &&
                                    yoloBox.getTop() > 0 &&
                                    yoloBox.getLeft() + yoloBox.getWidth() <= croppedBitmap.getWidth() &&
                                    yoloBox.getTop() + yoloBox.getHeight() <= croppedBitmap.getHeight()
                            ) { */
                   Log.e(LOGGING_TAG, "LEFT:" + (yoloBox.getLeft() + yoloBox.getWidth() / 2));
                   Log.e(LOGGING_TAG, "TOP:" + (yoloBox.getTop() + yoloBox.getHeight() / 2));
                   Log.e(LOGGING_TAG, yoloBox.toString());
                   Log.e(LOGGING_TAG, "imageBitamp " + croppedBitmap.getWidth() + " + " + croppedBitmap.getHeight());
                   float offsetX = 0;
                   float offsetY = 0;

                   float left = Math.max(0, yoloBox.getLeft() + offsetX);
                   float top = Math.max(offsetY, yoloBox.getTop() + offsetY);

                   float right = Math.min(yoloBox.getRight(), 416);
                   float bottom = Math.min(yoloBox.getBottom() + offsetY, 416);

                   Bitmap elBitmap = Bitmap.createBitmap(
                           croppedBitmap,
                           Math.round(left),
                           Math.round(top),
                           Math.round(right - left),
                           Math.round(bottom - top),
                           null,
                           false
                   );

                   Bitmap edgeBmp = Canny.detectEdges(elBitmap);
                   overlayView.elbitmap = edgeBmp;
                   requestRender();
                   widthInPix = EdgeMeasurer.getWidth(edgeBmp);

                   lastDistanceProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
                   Log.e("CUANTO ES IO?", Long.toString(lastDistanceProcessingTimeMs));
                   lastDistance = DistanceCalculator.calculateDistance(widthInPix, 4318, focalLength);

               }
        });

        this.textToWrite += "\nLastDistanceProcessingTimeMs:" + lastDistanceProcessingTimeMs + "\n"+
                "lastProcessingTime: " + lastProcessingTimeMs + "\n" +
                "Distance: " + lastDistance;

        logSuccessful = writeToFile(textToWrite);
        if((currentThreadTime - startActivityTime) > timeToCompleteLog){
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("wasLogSuccessful", logSuccessful);
            startActivity(intent);
        }
    }

    private boolean writeToFile(String data) {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        if(!dir.exists())
            dir.mkdirs();
        File file = new File(dir,"Log.txt");
        if(file.exists() && file.isFile()) {
            file.delete();
        }
        try (FileWriter fileWriter = new FileWriter(file,true)) {
            fileWriter.write(data);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void fillCroppedBitmap(final Image image) {
            Bitmap rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
            rgbFrameBitmap.setPixels(ImageUtils.convertYUVToARGB(image, previewWidth, previewHeight),
                    0, previewWidth, 0, 0, previewWidth, previewHeight);
            new Canvas(imageBitmap).drawBitmap(rgbFrameBitmap,     imageFrameToCropTransform , null);
            new Canvas(croppedBitmap).drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (recognizer != null) {
            recognizer.close();
        }
    }

    private void renderAdditionalInformation(final Canvas canvas) {
        final Vector<String> lines = new Vector();

        lines.add("");
        lines.add("Frame: " + previewWidth + "x" + previewHeight);
        lines.add("View: " + canvas.getWidth() + "x" + canvas.getHeight());
        lines.add("Rotation: " + sensorOrientation);
        lines.add("Inference time: " + lastProcessingTimeMs + "ms");
        lines.add("Distance Calculation Time:" + lastDistanceProcessingTimeMs );
        lines.add("Focal Length: "+ focalLength);
        lines.add("D1: " + lastDistance);

        borderedText.drawLines(canvas, 10, 10, lines);
    }


}

