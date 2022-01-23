package com.angmolin.livechess2fen.chessboard;

import com.angmolin.livechess2fen.chessboard.types.CLAHESettings;
import com.angmolin.livechess2fen.types.ImageObject;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Slid {

    private HashMap<Integer, double[]> hashMap;
    private HashMap<Integer, List<Integer>> hashGroup;
    private HashMap<Integer, Integer> hashX;

    private static Mat simplifyMat(Mat inputMat, double limit, Size grid, int iterations) {
        if (inputMat == null || grid == null) {
            return null;
        }

        Mat grayMat = new Mat(inputMat.size(), CvType.CV_8U);
        Imgproc.cvtColor(inputMat, grayMat, Imgproc.COLOR_RGBA2GRAY);
        Mat outputMat = grayMat;

        for (int i = 0; i < iterations; i++) {
            Imgproc.createCLAHE(limit, grid).apply(outputMat, outputMat);
        }

        return outputMat;
    }

    private static Mat detectEdges(Mat inputMat) {
        if (inputMat == null) {
            return null;
        }

        double sigma = 0.25;

        Mat shortedMat = new Mat();
        Core.sort(inputMat, shortedMat, Core.SORT_EVERY_COLUMN + Core.SORT_ASCENDING);

        double v = (shortedMat.get(shortedMat.height() / 2, 0)[0] + shortedMat.get(shortedMat.height() / 2 + 1, 0)[0]) / 2;

        Mat blurredMat = new Mat(inputMat.size(), inputMat.type());

        Imgproc.medianBlur(inputMat, blurredMat, 5);
        Imgproc.GaussianBlur(inputMat, blurredMat, new Size(7, 7),2);

        double lower = Math.max(0, (1.0 - sigma) * v);
        double upper = Math.min(255, (1.0 + sigma) * v);

        Mat outputMat = new Mat(blurredMat.size(), blurredMat.type());
        Imgproc.Canny(blurredMat, outputMat, lower, upper);

        blurredMat.release();

        return outputMat;
    }

    private static Mat detectLines(Mat inputMat) {
        int beta = 2;

        Mat outputMat = new Mat();
        Imgproc.HoughLinesP(inputMat, outputMat, 1, Math.PI / 360 * beta, 40, 50, 15);

        return outputMat;
    }

    public static Mat slidSegments(Mat image) {
        CLAHESettings claheSettings[] = {
                new CLAHESettings(3, new Size(2, 6), 5),
                new CLAHESettings(3, new Size(6, 2), 5),
                new CLAHESettings(5, new Size(3, 3), 5),
                new CLAHESettings(0, new Size(0, 0), 0),
        };

        Mat ocvSegments = new Mat();

        for (CLAHESettings settings : claheSettings) {
            Mat simplifiedImage = simplifyMat(image, settings.limit, settings.grid, settings.iterations);
            Mat edgesImage = detectEdges(simplifiedImage);
            Mat segmentsMat = detectLines(edgesImage);

            ocvSegments.push_back(segmentsMat);
        }

        return ocvSegments;
    }

    private static double segmentLength(double[] segment) {
        return Math.sqrt(Math.pow(segment[0] - segment[2], 2) + Math.pow(segment[1] - segment[3], 2));
    }

    private static double distance(double[] segment1, double[] segment2, int point, double dx) {
        return Math.abs(
                (segment1[2] - segment1[0]) * (segment1[1] - segment2[1 + 2 * point]) - (segment1[3] - segment1[1]) * (segment1[0] - segment2[0 + 2 * point])
        ) / dx;
    }

    private Integer findInHashMap(int k) {
        Integer v = hashX.get(k);

        if (v == null) {
            hashX.put(k, k);
            return k;
        }
        else if (v == k) {
            return k;
        }
        else {
            v = findInHashMap(v);
            hashX.put(k, v);
            return v;
        }
    }

    private void unionHashGroup(int a, int b) {
        Integer ia = findInHashMap(a);
        Integer ib = findInHashMap(b);

        hashX.put(ia, ib);
        hashGroup.get(ib).addAll(hashGroup.get(ia));
    }

    private static boolean similarSegments(double[] segment1, double[] segment2) {
        if (segment1 == null || segment2 == null) {
            return false;
        }

        double da = segmentLength(segment1);
        double db = segmentLength(segment2);

        double d1a = distance(segment1, segment2, 0, da);
        double d2a = distance(segment1, segment2, 1, da);
        double d1b = distance(segment2, segment1, 0, db);
        double d2b = distance(segment2, segment1, 1, db);

        double avgDev = 0.25 * (d1a + d1b + d2a + d2b) + 0.00001;
        double delta = 0.0625 * (da + db);

        return da / avgDev > delta && db / avgDev > delta;
    }

    private void joinGroups(List<double[]> segments) {
        int n = segments.size();

        for (int i = 0; i < n; i++) {
            double[] l1 = segments.get(i);
            int h1 = l1.hashCode();

            if (hashX.get(h1) != h1) {
                continue;
            }

            for (int j = i + 1; j < n; j++) {
                double[] l2 = segments.get(j);
                int h2 = l2.hashCode();

                if (hashX.get(h2) != h2) {
                    continue;
                }

                if (similarSegments(l1, l2)) {
                    unionHashGroup(h1, h2);
                }
            }
        }
    }

    private static Mat generatePoints(double[] segment, int n) {
        Mat points = new Mat(n, 1, CvType.CV_32FC2);

        double t = 1.0 / n;

        for (int i = 0; i < n; i++) {
            double it = i * t;

            double[] xy = new double[] {
                    segment[0] + (segment[2] - segment[0]) * it,
                    segment[1] + (segment[3] - segment[1]) * it
            };

            points.put(i, 0, xy);
        }

        return points;
    }

    private double[] mergeSegments(List<Integer> group) {
        Mat points = new Mat();

        for (Integer h : group) {
            double[] segment = hashMap.get(h);
            points.push_back(generatePoints(segment, 10));
        }

        MatOfPoint2f points2f = new MatOfPoint2f(points);


        float[] radiusOut = new float[1];
        Imgproc.minEnclosingCircle(points2f, null, radiusOut);

        double w = radiusOut[0] * (Math.PI / 2);

        Mat segmentOut = new Mat();
        Imgproc.fitLine(points2f, segmentOut, Imgproc.CV_DIST_L2, 0, 0.01, 0.01);

        double x1 = segmentOut.get(0, 0)[0];
        double y1 = segmentOut.get(1, 0)[0];
        double x2 = segmentOut.get(2, 0)[0];
        double y2 = segmentOut.get(3, 0)[0];


        return new double[] {
                x2 - x1 * w,
                y2 - y1 * w,
                x2 + x1 * w,
                y2 + y1 * w
        };
    }

    public static double scale(double v1, double v2, double s) {
        return v1 * (1 + s) / 2 + v2 * (1 - s) / 2;
    }

    public static double[] scaleSegment(double[] segment) {
        double scale = 4;

        return new double[] {
                scale(segment[0], segment[2], scale),
                scale(segment[1], segment[3], scale),
                scale(segment[2], segment[0], scale),
                scale(segment[3], segment[1], scale)
        };
    }

    public List<double[]> slid(Mat image) {
        hashMap = new HashMap<>();
        hashGroup = new HashMap<>();
        hashX = new HashMap<>();

        Mat segments = slidSegments(image);

        List<double[]> verticalSegments = new LinkedList<>();
        List<double[]> horizontalSegments = new LinkedList<>();

        for (int i = 0; i < segments.height(); i++) {
            double[] segment = segments.get(i, 0);
            int segmentHash = segment.hashCode();

            hashMap.put(segmentHash, segment);

            List<Integer> group = new LinkedList<>();
            group.add(segmentHash);

            hashGroup.put(segmentHash, group);

            hashX.put(segmentHash, segmentHash);

            double t1 = segment[0] - segment[2];
            double t2 = segment[1] - segment[3];

            if (Math.abs(t1) < Math.abs(t2)) {
                verticalSegments.add(segment);
            }
            else {
                horizontalSegments.add(segment);
            }
        }

        joinGroups(verticalSegments);
        joinGroups(horizontalSegments);

        List<double[]> finalSegments = new LinkedList<>();

        for (Map.Entry<Integer, List<Integer>> group : hashGroup.entrySet()) {
            Integer groupKey = group.getKey();

            if (!hashX.get(groupKey).equals(groupKey))
                continue;

            finalSegments.add(mergeSegments(group.getValue()));
        }

        return finalSegments;
    }

}
