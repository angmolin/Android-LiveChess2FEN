package com.angmolin.livechess2fen.classifier;

import org.opencv.core.Mat;

public interface ImageClassifier {

    int classifyAndGetMax(Mat image);

    float[] classifyAndGetResult(Mat image);

}
