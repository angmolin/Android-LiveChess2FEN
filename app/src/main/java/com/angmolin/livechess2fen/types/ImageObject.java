package com.angmolin.livechess2fen.types;

import com.angmolin.livechess2fen.chessboard.ChessboardDetector;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class ImageObject {

    private final List<ComplexMat> items;
    private final List<List<double[]>> points;

    public ImageObject(Mat image) {
        items = new ArrayList<>();
        points = new ArrayList<>();

        if (image != null) {
            items.add(new ComplexMat(image));
        }
    }

    public ComplexMat first() {
        if (items.size() > 0) {
            return items.get(0);
        }

        return null;
    }

    public ComplexMat last() {
        if (items.size() > 0) {
            return items.get(items.size() - 1);
        }

        return null;
    }

    public List<double[]> lastFourPoints() {
        if (points.size() > 0) {
            return points.get(points.size() - 1);
        }

        return null;
    }

    private void addMat(Mat mat) {
        items.add(new ComplexMat(mat));
    }

    public void addPoints(List<double[]> fourPoints) {
        points.add(fourPoints);
    }

    public List<List<double[]>> getPoints() {
        return points;
    }

    public void crop(List<double[]> points) {
        scalePoints(points, last().scale);
        Mat matCropped = matTransform(last().original, points);

        addPoints(points);
        addMat(matCropped);
    }

    public static void scalePoints(List<double[]> points, double scale) {
        for (double[] point : points) {
            for (int i = 0; i < point.length; i++) {
                point[i] /= scale;
            }
        }
    }

    public static Mat matTransform(Mat mat, List<double[]> points) {
        Size warpedSize = new Size(ChessboardDetector.boardLength, ChessboardDetector.boardLength);

        Mat warpedMat;

        if (points.size() > 0) {
            Mat fromPerspective = new Mat(4, 1, CvType.CV_32FC2);
            Mat toPerspective = new Mat(4, 1, CvType.CV_32FC2);
            fromPerspective.put(0, 0,
                    points.get(0)[0],  points.get(0)[1],
                    points.get(1)[0],  points.get(1)[1],
                    points.get(2)[0],  points.get(2)[1],
                    points.get(3)[0],  points.get(3)[1]
            );
            toPerspective.put(0, 0,
                    0.0,                            0.0,
                    ChessboardDetector.boardLength,                            0.0,
                    ChessboardDetector.boardLength, ChessboardDetector.boardLength,
                    0.0, ChessboardDetector.boardLength
            );
            Mat perspectiveTransform = Imgproc.getPerspectiveTransform(fromPerspective, toPerspective);

            warpedMat = new Mat(warpedSize, CvType.CV_8UC4);
            Imgproc.warpPerspective(mat, warpedMat, perspectiveTransform, warpedSize);
        }
        else {
            warpedMat = mat;
        }

        return warpedMat;
    }

}
