package com.angmolin.livechess2fen.chessboard;

import android.util.Pair;

import com.angmolin.livechess2fen.classifier.ImageClassifier;
import com.angmolin.livechess2fen.types.ImageObject;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ChessboardDetector {

    public static final int boardLength = 1200;

    public final Slid slid;
    public final Laps laps;

    public ChessboardDetector(ImageClassifier lapsClassifier) {
        slid = new Slid();
        laps = new Laps(lapsClassifier);
    }

    private Pair<List<double[]>, List<double[]>> originalPointsCoords(List<List<double[]>> points) {
        Size warpedSize = new Size(this.boardLength, this.boardLength);

        Mat toPerspective = new Mat(4, 1, CvType.CV_32FC2);
        toPerspective.put(0, 0,
                       0.0,              0.0,
                this.boardLength,              0.0,
                this.boardLength, this.boardLength,
                             0.0, this.boardLength
        );

        Mat perspectiveTransform;
        List<Mat> transformMatrices = new ArrayList<>();

        for (int i = 0; i < points.size() - 1; i++) {
            List<double[]> fourPoints = points.get(i);

            if (fourPoints.size() > 0) {
                Mat fromPerspective = new Mat(4, 1, CvType.CV_32FC2);
                fromPerspective.put(0, 0,
                        fourPoints.get(0)[0], fourPoints.get(0)[1],
                        fourPoints.get(1)[0], fourPoints.get(1)[1],
                        fourPoints.get(2)[0], fourPoints.get(2)[1],
                        fourPoints.get(3)[0], fourPoints.get(3)[1]
                );

                perspectiveTransform = Imgproc.getPerspectiveTransform(fromPerspective, toPerspective);

                Core.invert(perspectiveTransform, perspectiveTransform);
                transformMatrices.add(perspectiveTransform);
            }
        }

        if (transformMatrices.size() > 0) {
            perspectiveTransform = transformMatrices.get(0);
            for (int i = 1; i < transformMatrices.size(); i++) {
                Core.gemm(transformMatrices.get(i), perspectiveTransform, 1.0, new Mat(), 0.0, perspectiveTransform);
            }

            List<double[]> lastPoints = points.get(points.size() - 1);

            Mat lastPointsMat = new Mat(4, 1, CvType.CV_32FC2);
            lastPointsMat.put(0, 0,
                    lastPoints.get(0)[0], lastPoints.get(0)[1],
                    lastPoints.get(1)[0], lastPoints.get(1)[1],
                    lastPoints.get(2)[0], lastPoints.get(2)[1],
                    lastPoints.get(3)[0], lastPoints.get(3)[1]
            );

            Mat lastPointsOut = new Mat();
            Core.perspectiveTransform(lastPointsMat, lastPointsOut, perspectiveTransform);

            Mat lastPerspectiveTransform = Imgproc.getPerspectiveTransform(lastPointsMat, toPerspective);
            Core.invert(lastPerspectiveTransform, lastPerspectiveTransform);
            Core.gemm(lastPerspectiveTransform, perspectiveTransform, 1.0, new Mat(), 0.0, perspectiveTransform);

            List<double[]> corners = new LinkedList<>();
            int step = this.boardLength / 8;
            for (int rowCorner = step; rowCorner < this.boardLength; rowCorner += step) {
                for (int colCorner = step; colCorner < this.boardLength; colCorner += step) {
                    corners.add(new double[]{rowCorner, colCorner});
                }
            }

            Mat cornersMat = new Mat(corners.size(), 1, CvType.CV_32FC2);
            for (int i = 0; i < corners.size(); i++) {
                cornersMat.put(i, 0,
                        corners.get(i)[0], corners.get(i)[1]
                );
            }

            Mat cornersOut = new Mat();
            Core.perspectiveTransform(cornersMat, cornersOut, perspectiveTransform);

            List<double[]> fourPointsResult = new ArrayList<>();
            for (int i = 0; i < lastPointsOut.rows(); i++) {
                fourPointsResult.add(lastPointsOut.get(i, 0));
            }

            List<double[]> cornersResult = new ArrayList<>();
            for (int i = 0; i < cornersOut.rows(); i++) {
                cornersResult.add(cornersOut.get(i, 0));
            }

            return new Pair<>(fourPointsResult, cornersResult);
        }

        return new Pair<>(new ArrayList<>(), new ArrayList<>());
    }

    private void layer(ImageObject image) {
        // Step 1 - Straight line detector (SLID)
        List<double[]> segments = slid.slid(image.last().downscaled);
        if (segments == null || segments.size() > 200) {
            return;
        }

        // Step 2 - Lattice points search (LAPS)
        List<double[]> points = laps.laps(image.last().downscaled, segments);
        if (points == null || points.size() > 200 ) {
            return;
        }

        // Step 3 - Chessboard position search (CPS)
        List<double[]> fourPoints = Cps.cps(image.last().downscaled, points, segments);
        if (fourPoints == null || fourPoints.size() > 4) {
            return;
        }

        // Step 4 - Crop the image
        image.crop(fourPoints);
    }

    public ImageObject detect(Mat image, List<double[]> fourPoints) {
        ImageObject imageObject = new ImageObject(image);

        if (fourPoints != null && fourPoints.size() == 4) {
            boolean found = laps.checkBoardPosition(image, fourPoints, 20);

            if (found) {
                imageObject.addPoints(Arrays.asList(
                        new double[] {                0,                0 },
                        new double[] { this.boardLength,                0 },
                        new double[] { this.boardLength, this.boardLength },
                        new double[] {                0, this.boardLength }
                ));
                imageObject.addPoints(fourPoints);
            }
        }

        int nLayers = 3;
        for (int i = 0; i < nLayers; i++) {
            layer(imageObject);
        }

        return imageObject;
    }

    public Pair<List<double[]>, List<double[]>> computeCorners(ImageObject image) {
        return originalPointsCoords(image.getPoints());
    }
}
