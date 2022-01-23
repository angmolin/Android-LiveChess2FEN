package com.angmolin.livechess2fen;

import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

public class BoardSplitter {

    private static final int numSquares = 8;

    public static List<Mat> splitSquaresFromBoardImage(Mat image) {
        List<Mat> squares = new ArrayList<>();

        if (image.rows() != image.cols()) {
            throw new IllegalArgumentException("Mat rows should be equal to cols");
        }

        double squareSize = image.rows() / numSquares;
        for (int i = 0; i < image.rows(); i += squareSize) {
            for (int j = 0; j < image.cols(); j += squareSize) {
                Mat square = image.submat(
                        i,
                        (int)(i + squareSize),
                        j,
                        (int)(j + squareSize)
                );

                squares.add(square);
            }
        }

        return squares;
    }

    public static List<Mat> splitBoardImage(Mat image, List<double[]> corners) {
        List<Mat> squares = new ArrayList<>();

        for (int i = 1; i < 9; i++) {
            for (int j = 0; j < 8; j++) {
                double[] blCorner = corners.get( i + 9  *  j);
                double[] brCorner = corners.get( i + 9  * (j + 1));
                double[] tlCorner = corners.get((i - 1) +  9 * j);

                int height = (int)((blCorner[1] - tlCorner[1]) * 1.75);

                if (blCorner[1] - height < 0 || brCorner[1] - height < 0) {
                    height = Math.min((int)blCorner[1], (int)brCorner[1]);
                }

                Mat square = image.submat(
                        (int)(blCorner[1] - height),
                        (int)(blCorner[1]),
                        (int)(blCorner[0]),
                        (int)(brCorner[0])
                );

                squares.add(square);
            }
        }

        return squares;
    }

}
