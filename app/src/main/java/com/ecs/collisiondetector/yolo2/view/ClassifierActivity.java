package com.ecs.collisiondetector.yolo2.view;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;

import com.ecs.collisiondetector.DistanceCalculator;
import com.ecs.collisiondetector.EdgeDetection.Canny;
import com.ecs.collisiondetector.EdgeDetection.EdgeMeasurer;
import com.ecs.collisiondetector.yolo2.TensorFlowImageRecognizer;
import com.ecs.collisiondetector.yolo2.model.Recognition;
import com.ecs.collisiondetector.yolo2.util.ImageUtils;

import com.ecs.collisiondetector.R;

import com.ecs.collisiondetector.yolo2.view.components.BorderedText;

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

        int pixelWidth = EdgeMeasurer.getWidth(croppedBitmap);

        DistanceCalculator.calculateDistance(pixelWidth, 1871, 35);

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

        runInBackground(() -> {
            final long startTime = SystemClock.uptimeMillis();
            final List<Recognition> results = recognizer.recognizeImage(croppedBitmap);

            Log.e(LOGGING_TAG, results.toString());
            if(results.size()>0) {
                final Recognition firstResult = results.get(0);
                Float width = firstResult.getLocation().getRight();
                Float height = firstResult.getLocation().getBottom();
                Bitmap elBitmap = Bitmap.createBitmap(Math.round(width), Math.round(height), Config.ARGB_8888);
                Bitmap edgeBmp = Canny.detectEdges(elBitmap);
                int widthInPix = EdgeMeasurer.getWidth(edgeBmp);
                DistanceCalculator distanceCalculator1 = new DistanceCalculator();
                DistanceCalculator distanceCalculator2 = new DistanceCalculator();
                distanceCalculator1.setFocalLength(26);
                distanceCalculator2.setFocalLength(35);
                Log.e(LOGGING_TAG,"Calculator1: "+ distanceCalculator1.calculateDistance(widthInPix, 4318));
                Log.e(LOGGING_TAG,"Calculator2: "+distanceCalculator1.calculateDistance(widthInPix, 4318));
            }
            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
            overlayView.setResults(results);
            speak(results);
            requestRender();
            computing = false;
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
        if (recognizer != null) {
            for (String line : recognizer.getStatString().split("\n")) {
                lines.add(line);
            }
        }

        lines.add("Frame: " + previewWidth + "x" + previewHeight);
        lines.add("View: " + canvas.getWidth() + "x" + canvas.getHeight());
        lines.add("Rotation: " + sensorOrientation);
        lines.add("Inference time: " + lastProcessingTimeMs + "ms");

        borderedText.drawLines(canvas, 10, 10, lines);
    }
}
