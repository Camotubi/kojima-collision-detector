package com.ecs.collisiondetector.yolo2.view;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.widget.Toast;

import com.ecs.collisiondetector.DistanceCalculator;
import com.ecs.collisiondetector.EdgeDetection.Canny;
import com.ecs.collisiondetector.EdgeDetection.EdgeMeasurer;
import com.ecs.collisiondetector.yolo2.TensorFlowImageRecognizer;
import com.ecs.collisiondetector.yolo2.model.BoxPosition;
import com.ecs.collisiondetector.yolo2.model.Recognition;
import com.ecs.collisiondetector.yolo2.util.ImageUtils;

import com.ecs.collisiondetector.R;

import com.ecs.collisiondetector.yolo2.view.components.BorderedText;

import org.apache.commons.math3.genetics.TournamentSelection;

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

    private OverlayView overlayView;
    private BorderedText borderedText;
    private long lastProcessingTimeMs;
    private long lastDistanceProcessingTimeMs = 0;
    private Double focalLength = 0.0;
    private int widthInPix = 0;

    private  List<Recognition> results;
    @Override
    public synchronized  void onCreate(Bundle bundle) {
        Intent intent = getIntent();
        focalLength = intent.getDoubleExtra("focalLength",0.0);

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


        frameToCropTransform = ImageUtils.getTransformationMatrix(previewWidth, previewHeight,
                INPUT_SIZE, INPUT_SIZE, sensorOrientation, MAINTAIN_ASPECT);
        frameToCropTransform.invert(new Matrix());

        addCallback((final Canvas canvas) -> renderAdditionalInformation(canvas));

    }

    @Override
    public void onImageAvailable(final ImageReader reader) {
        Image image = null;
        Activity selfActivity = ClassifierActivity.this;

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
        Log.e(LOGGING_TAG, "yolothread");
        runInBackground(() -> {
            final long startTime = SystemClock.uptimeMillis();
            results = recognizer.recognizeImage(croppedBitmap);
            Log.e(LOGGING_TAG, "inside yolo thread");
            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
            overlayView.setResults(results);
            requestRender();
            computing = false;
        });
        Log.e(LOGGING_TAG, "Cannythread");
        runInBackground2(() -> {
            while(true) {
                Log.e(LOGGING_TAG, "Waowao:" + widthInPix);
                if (results != null && results.size() > 0) {
                    final long startTime = SystemClock.uptimeMillis();
                    BoxPosition yoloBox = results.get(0).getLocation();
                    if (yoloBox.getLeft() + yoloBox.getWidth() <= croppedBitmap.getWidth() && yoloBox.getTop() + yoloBox.getHeight() <= croppedBitmap.getWidth()) {
                        Bitmap elBitmap = Bitmap.createBitmap(
                                croppedBitmap,
                                Math.abs(Math.round(yoloBox.getLeft())),
                                Math.abs(Math.round(yoloBox.getTop())),
                                Math.round(yoloBox.getWidth()),
                                Math.round(yoloBox.getHeight()),
                                null,
                                false
                        );

                        Bitmap edgeBmp = Canny.detectEdges(elBitmap);
                        widthInPix = EdgeMeasurer.getWidth(edgeBmp);
                        Log.e(LOGGING_TAG, "Width In Pix:" + widthInPix);
                    } else {
                        widthInPix = Math.round(yoloBox.getWidth());
                    }
                    lastDistanceProcessingTimeMs = SystemClock.uptimeMillis() - startTime;


                }
            }
        });


    }

    private void fillCroppedBitmap(final Image image) {
            Bitmap rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
            rgbFrameBitmap.setPixels(ImageUtils.convertYUVToARGB(image, previewWidth, previewHeight),
                    0, previewWidth, 0, 0, previewWidth, previewHeight);
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
        lines.add("D1: " + DistanceCalculator.calculateDistance(widthInPix,4318,focalLength));

        borderedText.drawLines(canvas, 10, 10, lines);
    }
}
