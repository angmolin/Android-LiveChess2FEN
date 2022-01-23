package com.angmolin.livechess2fen.chessboard;

import com.angmolin.livechess2fen.chessboard.algorithm.bentleyottmann.BentleyOttmann;
import com.angmolin.livechess2fen.chessboard.algorithm.bentleyottmann.types.BOPoint;
import com.angmolin.livechess2fen.chessboard.algorithm.bentleyottmann.types.BOSegment;
import com.angmolin.livechess2fen.classifier.ImageClassifier;
import com.angmolin.livechess2fen.types.ImageObject;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class Laps {

    private final int analysisRadius = 10;

    private final ImageClassifier lapsClassifier;

    public Laps(ImageClassifier lapsClassifier) {
        this.lapsClassifier = lapsClassifier;
    }

    private static List<BOPoint> clusterPoints(List<BOPoint> boPoints, int maximumDistance) {
        List<BOPoint> boGroups = new ArrayList<>();

        for (BOPoint boPoint : boPoints) {
            int i = 0;
            boolean found = false;

            while (i < boGroups.size() && !found) {
                BOPoint boGroup = boGroups.get(i);

                if (boGroup.distanceTo(boPoint) < maximumDistance) {
                    boGroups.set(i, new BOPoint(
                            (boGroup.x + boPoint.x) / 2,
                            (boGroup.y + boPoint.y) / 2
                    ));

                    found = true;
                }
                else {
                    i++;
                }
            }

            if (!found) {
                boGroups.add(boPoint);
            }
        }

        return boGroups;
    }

    private boolean isLatticePoint(Mat image) {
        int cols = image.cols();
        int rows = image.rows();

        Mat grayImage = new Mat(rows, cols, CvType.CV_8U);
        Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_RGBA2GRAY);

        Mat thresholdImage = new Mat(rows, cols, CvType.CV_8U);
        Imgproc.threshold(grayImage, thresholdImage, 0, 255, Imgproc.THRESH_OTSU);

        Mat cannyImage = new Mat(rows, cols, CvType.CV_8U);
        Imgproc.Canny(thresholdImage, cannyImage, 0, 255);

        Size kernelSize = new Size(1, 1); // 2 * kernelSize + 1
        Mat kernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, kernelSize); // , new Point(3, 3));

        Mat dilatedImage = new Mat(rows, cols, CvType.CV_8U);
        Imgproc.dilate(cannyImage, dilatedImage, kernel);

        Mat maskedImage = new Mat(rows + 2, cols + 2, CvType.CV_8U);
        Core.copyMakeBorder(dilatedImage, maskedImage, 1, 1, 1, 1, Core.BORDER_CONSTANT, new Scalar(255));
        Core.bitwise_not(maskedImage, maskedImage);

        Mat hierarchy = new Mat();
        List<MatOfPoint> contoursOut = new LinkedList<>();
        Imgproc.findContours(maskedImage, contoursOut, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

        int numRhomboid = 0;
        //Mat contoursImage = new Mat(rows + 2, cols + 2, CvType.CV_8UC4);

        for (MatOfPoint contour : contoursOut) {
            MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());

            float[] radiusOut = new float[1];
            Imgproc.minEnclosingCircle(contour2f, null, radiusOut);

            MatOfPoint2f approxOut = new MatOfPoint2f();
            Imgproc.approxPolyDP(contour2f, approxOut, 0.1 * Imgproc.arcLength(contour2f, true), true);

            //List<MatOfPoint> contourList = new LinkedList<>();
            //contourList.add(contour);

            if (approxOut.rows() == 4 && radiusOut[0] < 14) {
                numRhomboid++;
            }

            //Imgproc.drawContours(contoursImage, contourList, 0, new Scalar(0, 255, 0), 1);
        }

        if (numRhomboid == 4) {
            return true;
        }

        if (lapsClassifier != null) {
            return lapsClassifier.classifyAndGetMax(cannyImage) == 0;
        }

        return false;
    }

    private List<BOPoint> analyzePoints(Mat image, List<BOPoint> intersectionPoints) {
        int cols = image.cols();
        int rows = image.rows();

        List<BOPoint> chessboardPoints = new LinkedList<>();

        for (BOPoint p : intersectionPoints) {
            if (p.x < 0 || p.y < 0 || p.x > cols || p.y > cols) {
                continue;
            }

            int lX1 = Math.max(0, (int) p.x - analysisRadius - 1);
            int lX2 = Math.max(0, (int) p.x + analysisRadius);
            int lY1 = Math.max(0, (int) p.y - analysisRadius);
            int lY2 = Math.max(0, (int) p.y + analysisRadius + 1);

            Mat pMat = new Mat();

            if (lX1 <= image.cols() && lY1 <= image.rows()) {
                lX2 = Math.min(cols, lX2);
                lY2 = Math.min(rows, lY2);

                pMat = image.submat(new Range(lY1, lY2), new Range(lX1, lX2));
            }

            if (pMat.rows() <= 0 || pMat.cols() <= 0) {
                continue;
            }

            if (!isLatticePoint(pMat)) {
                continue;
            }
            else {
                Imgproc.circle(image, new Point(p.x, p.y), 5, new Scalar(255, 255, 255), -1);
            }

            chessboardPoints.add(p);
        }

        return chessboardPoints;
    }

    public List<double[]> laps(Mat image, List<double[]> segments) {
        BentleyOttmann bo = new BentleyOttmann(rawToBOSegments(segments));
        bo.findIntersections();
        List<BOPoint> intersectionPoints = bo.getIntersections();

        List<BOPoint> chessboardPoints;
        
        chessboardPoints = analyzePoints(image, intersectionPoints);

        /*
         * FIXME Cluster points have been implemented but it's not
         *  working properly so I have disabled by now.
         */
        List<BOPoint> clusteredPoints = clusterPoints(chessboardPoints, 5);

        List<double[]> result = new LinkedList<>();
        for (BOPoint point : chessboardPoints) {
            result.add(point.toRaw());
        }

        return result;
    }

    public boolean checkBoardPosition(Mat image, List<double[]> fourPoints, int tolerance) {
        Mat croppedMat = ImageObject.matTransform(image, fourPoints);

        int cols = croppedMat.cols();
        int rows = croppedMat.rows();

        int correctPoints = 0;
        int step = ChessboardDetector.boardLength / 8;
        for (int rowCorner = step; rowCorner < ChessboardDetector.boardLength; rowCorner += step) {
            for (int colCorner = step; colCorner < ChessboardDetector.boardLength; colCorner += step) {
                int lX1 = Math.max(0, rowCorner - analysisRadius - 1);
                int lX2 = Math.max(0, rowCorner + analysisRadius);
                int lY1 = Math.max(0, colCorner - analysisRadius);
                int lY2 = Math.max(0, colCorner + analysisRadius + 1);

                Mat pMat = new Mat();

                if (lX1 <= image.cols() && lY1 <= image.rows()) {
                    lX2 = Math.min(cols, lX2);
                    lY2 = Math.min(rows, lY2);

                    pMat = croppedMat.submat(new Range(lY1, lY2), new Range(lX1, lX2));
                }

                if (pMat.rows() <= 0 || pMat.cols() <= 0) {
                    continue;
                }

                if (isLatticePoint(pMat)) {
                    correctPoints++;
                }
            }
        }

        return correctPoints >= tolerance;
    }

    private static List<BOSegment> rawToBOSegments(List<double[]> rawSegments) {
        List<BOSegment> boSegments = new ArrayList<>();

        for (double[] rawSegment : rawSegments) {
            boSegments.add(BOSegment.fromRawData(rawSegment));
        }

        return boSegments;
    }
}
