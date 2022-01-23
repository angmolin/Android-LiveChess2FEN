package com.angmolin.livechess2fen.types;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class ComplexMat {

    public final Mat original;
    public final Mat downscaled;
    public final double scale;

    public ComplexMat(Mat mat, int height) {
        original = mat.clone();
        scale = Math.sqrt(
                (double) (height * height) /
                        (double) (original.rows() * original.cols())
        );

        Size downscaledSize = new Size(
                original.cols() * scale,
                original.rows() * scale
        );
        downscaled = new Mat(downscaledSize, mat.type());

        Imgproc.resize(original, downscaled, downscaledSize, 0, 0);
    }

    public ComplexMat(Mat mat) {
        this(mat, 500);
    }

}
