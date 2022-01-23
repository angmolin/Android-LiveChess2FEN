package com.angmolin.livechess2fen.classifier;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class TFImageClassifier implements ImageClassifier {

    private final Interpreter interpreter;
    private final ByteBuffer inputImage;

    private final String path;

    private final float confidenceThreshold;
    private final int batchSize;
    private final Size imageSize;
    private final int pixelSize;
    private final int modelIndices;

    private final int[] intValues;
    private final float[][] outputArray;

    public TFImageClassifier(AssetManager assetManager, String path, float confidenceThreshold, int batchSize, Size imageSize, int pixelSize, int modelIndices, int numThreads) throws IOException {
        this.path = path;

        this.confidenceThreshold = confidenceThreshold;
        this.batchSize = batchSize;
        this.imageSize = imageSize;
        this.pixelSize = pixelSize;
        this.modelIndices = modelIndices;

        intValues = new int[(int)(imageSize.width * imageSize.height)];
        outputArray = new float[this.batchSize][this.modelIndices];

        interpreter = new Interpreter(mapFileInMemory(assetManager), numThreads);
        inputImage = ByteBuffer.allocateDirect(4
                        * batchSize
                        * (int)imageSize.width
                        * (int)imageSize.height
                        * pixelSize);
        inputImage.order(ByteOrder.nativeOrder());
    }

    private MappedByteBuffer mapFileInMemory(AssetManager assetManager) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(this.path);

        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();

        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (inputImage == null) {
            return;
        }

        inputImage.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        int pixel = 0;
        long startTime = SystemClock.uptimeMillis();

        for (int i = 0; i < (int)imageSize.width; i++) {
            for (int j = 0; j < (int)imageSize.height; j++) {
                final int val = intValues[pixel++];

                if (pixelSize == 3) {
                    inputImage.putFloat(((val >> 16) & 0xFF) / 255.0f);
                }

                if (pixelSize >= 2) {
                    inputImage.putFloat(((val >> 8) & 0xFF) / 255.0f);
                }

                if (pixelSize >= 1) {
                    inputImage.putFloat(((val) & 0xFF) / 255.0f);
                }
            }
        }

        long endTime = SystemClock.uptimeMillis();
        Log.d("TFImageClassifier", "Timecost to put values into ByteBuffer: " + (endTime - startTime));
    }

    private void convertMatToByteBuffer(Mat image) {
        if (interpreter == null) {
            Log.e("TFImageClassifier", "Image classifier has not been initialized; Skipped.");
            return;
        }

        if (image.rows() != imageSize.height || image.cols() != imageSize.width) {
            Mat resizedImage = new Mat(imageSize, image.type());
            Imgproc.resize(image, resizedImage, imageSize, 0, 0, Imgproc.INTER_CUBIC);

            image = resizedImage;
        }

        Bitmap bitmap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.RGB_565);
        org.opencv.android.Utils.matToBitmap(image, bitmap);

        convertBitmapToByteBuffer(bitmap);
    }

    private void runInference() {
        long startTime = SystemClock.uptimeMillis();
        interpreter.run(inputImage, outputArray);
        long endTime = SystemClock.uptimeMillis();
        Log.d("TFImageClassifier", "Timecost to run model inference: " + (endTime - startTime));
    }

    private void classify(Mat image) {
        convertMatToByteBuffer(image);
        runInference();
    }

    private int getMaximum() {
        int maxIndex = -1;
        float maxProb = confidenceThreshold;

        for (int i = 0; i < outputArray[0].length; i++) {
            if (outputArray[0][i] >= maxProb) {
                maxProb = outputArray[0][i];
                maxIndex = i;
            }
        }

        return maxIndex;
    }

    @Override
    public int classifyAndGetMax(Mat image) {
        classify(image);
        return getMaximum();
    }

    @Override
    public float[] classifyAndGetResult(Mat image) {
        classify(image);

        float[] probabilities = new float[this.modelIndices];
        for (int i = 0; i < outputArray[0].length; i++) {
            probabilities[i] = outputArray[0][i];
        }

        return probabilities;
    }

}
