package com.angmolin.livechess2fen.chessboard;

import android.util.Pair;

import com.angmolin.livechess2fen.chessboard.algorithm.dbscan.DBSCAN;
import com.angmolin.livechess2fen.chessboard.algorithm.dbscan.exceptions.DBException;
import com.angmolin.livechess2fen.chessboard.algorithm.dbscan.metrics.DBMetricEuclidean;
import com.angmolin.livechess2fen.chessboard.algorithm.dbscan.types.DBPoint;
import com.angmolin.livechess2fen.chessboard.algorithm.dbscan.types.DBSegment;
import com.angmolin.livechess2fen.types.ImageObject;

import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import de.lighti.clipper.*;
import de.lighti.clipper.Point.*;

public class Cps {

    private static void sortPoints(List<DBPoint> dbPoints) {
        double lat = 0;
        double lng = 0;

        for (DBPoint dbPoint : dbPoints) {
            lat += dbPoint.x;
            lng += dbPoint.y;
        }

        lat /= dbPoints.size();
        lng /= dbPoints.size();

        for (DBPoint dbPoint : dbPoints) {
            dbPoint.latLng = (Math.atan2(dbPoint.x - lat, dbPoint.y - lng) + 2 * Math.PI) % (2 * Math.PI);
        }

        Collections.sort(dbPoints, (o1, o2) -> o1.latLng.compareTo(o2.latLng));
    }

    private static MatOfPoint dbPointListToMatOfPoint(List<DBPoint> dbPoints) {
        MatOfPoint matOfPoint = new MatOfPoint();

        List<Point> points = new LinkedList<>();
        for (DBPoint dbPoint : dbPoints) {
            points.add(new Point(dbPoint.toRaw()));
        }

        matOfPoint.fromList(points);

        return matOfPoint;
    }

    private static List<Pair<DBSegment, DBSegment>> combinations(List<DBSegment> segments) {
        List<Pair<DBSegment, DBSegment>> combinations = new LinkedList<>();

        for (int i = 0; i < segments.size() - 1; i++) {
            for (int j = i + 1; j < segments.size(); j++) {
                combinations.add(
                        new Pair<>(
                                segments.get(i),
                                segments.get(j)
                        )
                );
            }
        }

        return combinations;
    }

    private static DBPoint[] orderPoints(List<DBPoint> points) {
        DBPoint[] rect = new DBPoint[] {
                new DBPoint(-1, -1),
                new DBPoint(-1, -1),
                new DBPoint(-1, -1),
                new DBPoint(-1, -1)
        };

        // The top-left point will have the smallest sum
        // The bottom-right point will have the largest sum
        // The top-right point will have the smallest difference
        // The bottom-left point will have the largest difference

        double smallestSum = Double.MAX_VALUE;
        double largestSum = Double.MIN_VALUE;
        double smallestDif = Double.MAX_VALUE;
        double largestDif = Double.MIN_VALUE;

        for (DBPoint p : points) {
            p.normalize();

            double sum = p.x + p.y;
            double dif = p.x - p.y;

            if (smallestSum > sum) {
                smallestSum = sum;
                rect[0] = p;
            } // Ok

            if (largestSum < sum) {
                largestSum = sum;
                rect[2] = p;
            } // Ok

            if (smallestDif > dif) {
                smallestDif = dif;
                rect[3] = p;
            }

            if (largestDif < dif) {
                largestDif = dif;
                rect[1] = p;
            }
        }

        return rect;
    }

    public static double polyScore(List<DBPoint> poly, List<DBPoint> points, DBPoint centroid, double alpha, double beta) {
        double polyArea = Imgproc.contourArea(dbPointListToMatOfPoint(poly));

        if (polyArea < 4 * alpha * alpha * 5) {
            return 0;
        }

        double gamma = alpha / 1.5;

        Path _poly = new Path();
        for (DBPoint dbPoint : poly) {
            _poly.add(new LongPoint((long) dbPoint.x, (long) dbPoint.y));
        }

        ClipperOffset clipperOffset = new ClipperOffset();
        clipperOffset.addPath(_poly, Clipper.JoinType.MITER, Clipper.EndType.CLOSED_POLYGON);

        Paths paths = new Paths();
        clipperOffset.execute(paths, gamma);

        Path path = paths.get(0);

        List<DBPoint> pcnt = new LinkedList<>();
        for (LongPoint p : path) {
            pcnt.add(new DBPoint(p.getX(), p.getY()));
        }

        List<DBPoint> wtfs = new LinkedList<>();
        for (DBPoint dbPoint : points) {
            LongPoint lPoint = new LongPoint((long) dbPoint.x, (long)dbPoint.y);

            if (path.isPointInPolygon(lPoint) != 0) {
                wtfs.add(dbPoint);
            }
        }

        int ptsInFrame = Math.min(wtfs.size(), 49);
        if (ptsInFrame == 0 || ptsInFrame < Math.min(points.size(), 49) - 2 * beta - 1) {
            return 0;
        }

        MatOfInt hullIndices = new MatOfInt();
        MatOfPoint hullPoints = dbPointListToMatOfPoint(wtfs);
        Imgproc.convexHull(hullPoints, hullIndices);

        List<Integer> hullIndicesList = hullIndices.toList();
        List<DBPoint> hullPointsList = new LinkedList<>();
        for (Integer i : hullIndicesList) {
            hullPointsList.add(wtfs.get(i));
        }

        int length = hullPointsList.size();

        int sumX = 0;
        int sumY = 0;

        for (DBPoint p : hullPointsList) {
            sumX += p.x;
            sumY += p.y;
        }

        Point cen2 = new Point(sumX / length, sumY / length);
        double cenDist = Math.sqrt(Math.pow(centroid.x - cen2.x, 2) + Math.pow(centroid.y - cen2.y, 2));

        DBSegment[] segments = {
                new DBSegment(poly.get(0), poly.get(1)),
                new DBSegment(poly.get(1), poly.get(2)),
                new DBSegment(poly.get(2), poly.get(3)),
                new DBSegment(poly.get(3), poly.get(0))
        };

        double i = 0;
        double j = 0;

        for (DBSegment segment : segments) {
            double d = Math.sqrt(Math.pow(segment.p1.x - segment.p2.x, 2) + Math.pow(segment.p1.y - segment.p2.y, 2));

            for (DBPoint p : hullPointsList) {
                double r = segment.distanceTo(p);

                if (r < gamma) {
                    i += r;
                    j += 1;
                }
            }
        }

        if (j == 0) {
            return 0;
        }

        double averageDist = i / j;

        if (polyArea == 0 || ptsInFrame == 0) {
            return 0;
        }

        double wPoints = 1 + Math.pow(averageDist / ptsInFrame, 0.333);
        double wCentroid = 1 + Math.pow(cenDist / ptsInFrame, 0.200);

        return Math.pow(ptsInFrame, 4) / (Math.pow(polyArea, 2) * wPoints * wCentroid);
    }

    private static void removeDuplicates(List<DBSegment> segments) {
        HashSet<DBSegment> cluster = new HashSet<>();
        cluster.addAll(segments);

        segments.clear();
        segments.addAll(cluster);
    }

    private static DBPoint[] padCrop(DBPoint[] dbPoints) {
        Path pathToPad = new Path();

        for (DBPoint dbPoint : dbPoints) {
            pathToPad.add(new LongPoint((long) dbPoint.x, (long) dbPoint.y));
        }

        ClipperOffset clipperOffset = new ClipperOffset();
        clipperOffset.addPath(pathToPad, Clipper.JoinType.MITER, Clipper.EndType.CLOSED_POLYGON);

        Paths paddedPaths = new Paths();
        clipperOffset.execute(paddedPaths, 60);

        Path paddedPath = paddedPaths.get(0);

        List<DBPoint> dbPointsPadded = new LinkedList<>();
        for (LongPoint p : paddedPath) {
            dbPointsPadded.add(new DBPoint(p.getX(), p.getY()));
        }

        return orderPoints(dbPointsPadded);
    }

    public static List<double[]> cps(Mat image, List<double[]> points, List<double[]> segments) {
        if (points.size() == 0) {
            return new LinkedList<>();
        }

        Size imageSize = image.size();

        List<DBPoint> dbPoints = new LinkedList<>();
        for (double[] point : points) {
            DBPoint dbPoint = DBPoint.fromRaw(point);
            dbPoint.normalize();

            if (dbPoint.checkCorrectness(imageSize)) {
                dbPoints.add(dbPoint);
            }
        }

        sortPoints(dbPoints);
        List<DBPoint> biggestGroup = null;

        double alpha = Math.sqrt(Imgproc.contourArea(dbPointListToMatOfPoint(dbPoints)) / 49);
        try {
            DBSCAN<DBPoint> dbscan = new DBSCAN<>(dbPoints, 5, alpha * 4, new DBMetricEuclidean());
            List<List<DBPoint>> dbGroups = dbscan.performClustering();

            for (List<DBPoint> dbGroup : dbGroups) {
                if (biggestGroup == null || biggestGroup.size() < dbGroup.size()) {
                    biggestGroup = dbGroup;
                }
            }

            List<DBPoint> finalPoints;
            if (dbGroups.size() > 0 && dbPoints.size() > 49 / 2) {
                finalPoints = biggestGroup;
            }
            else {
                finalPoints = dbPoints;
            }

            int n = finalPoints.size();
            double beta = n * 0.05;
            alpha = Math.sqrt(Imgproc.contourArea(dbPointListToMatOfPoint(finalPoints)) / 49);

            //List<Double> x = new LinkedList<>();
            double sumX = 0;
            //List<Double> y = new LinkedList<>();
            double sumY = 0;

            for (DBPoint dbPoint : finalPoints) {
                //x.add(dbPoint.x);
                sumX += dbPoint.x;

                //y.add(dbPoint.y);
                sumY += dbPoint.y;
            }
            DBPoint centroid = new DBPoint(sumX / n, sumY / n);

            List<DBSegment> dbSegments = new LinkedList<>();
            for (double[] segment : segments) {
                dbSegments.add(DBSegment.fromRawData(segment));
            }

            List<DBSegment> dbSegmentsHorizontal = new LinkedList<>();
            List<DBSegment> dbSegmentsVertical = new LinkedList<>();

            /*
             * FIXME This implementation is not totally correct but works well
             *  so I'm going to correct it in a near future.
             *
             * Notes:
             *   + dbSegmentsHorizontal contains the horizontal segments
             *   + dbSegmentsVertical contains the vertical segments
             *
             * These things have been tested because it's probably an easy to
             *  fix error
             */
            for (DBSegment dbSegment : dbSegments) {
                // Distance between line and centroid should be mayor than 2.5 * alpha
                if (dbSegment.distanceTo(centroid) > alpha * 2.5) {
                    for (DBPoint point : finalPoints) {
                        // Distance between line and point should be minor than alpha
                        if (dbSegment.distanceTo(point) < alpha) {
                            double t1 = dbSegment.first().x - dbSegment.second().x;
                            double t2 = dbSegment.first().y - dbSegment.second().y;

                            if (Math.abs(t1) < Math.abs(t2)) {
                                dbSegmentsVertical.add(dbSegment);
                            }
                            else {
                                dbSegmentsHorizontal.add(dbSegment);
                            }
                        }
                    }
                }
            }
            /*
            for (DBSegment dbSegment : dbSegments) {
                // Distance between line and centroid should be mayor than 2.5 * alpha
                if (dbSegment.distanceTo(centroid) > alpha * 2.5) {
                    for (DBPoint point : finalPoints) {
                        // Distance between line and point should be minor than alpha
                        if (dbSegment.distanceTo(point) < alpha) {
                            boolean vertical;

                            double s1;
                            double s2;

                            double x0 = dbSegment.first().x;
                            double y0 = dbSegment.first().y;
                            double x1 = dbSegment.second().x;
                            double y1 = dbSegment.second().y;

                            double x2 = 0;

                            double t;
                            DBPoint a, b;

                            List<DBPoint> poly1 = new LinkedList<>(), poly2 = new LinkedList<>();


                            if (dbSegment.deltaX < dbSegment.deltaY) {
                                t = (y0 - x2) / (y0 - y1 + 0.0001);
                                a = new DBPoint(
                                        (int)(1 - t) * x0 + t * x1,
                                        (int)(1 - t) * y0 + t * y1
                                );

                                x2 = imageSize.height;
                                t = (y0 - x2) / (y0 - y1 + 0.0001);
                                b = new DBPoint(
                                        (int)(1 - t) * x0 + t * x1,
                                        (int)(1 - t) * y0 + t * y1
                                );

                                poly1.add(new DBPoint(0, 0));
                                poly1.add(new DBPoint(0, imageSize.height));
                                poly1.add(a);
                                poly1.add(b);

                                poly2.add(a);
                                poly2.add(b);
                                poly2.add(new DBPoint(imageSize.width, 0));
                                poly2.add(new DBPoint(imageSize.width, imageSize.height));

                                vertical = true;
                            }
                            else {
                                t = (x0 - x2) / (x0 - x1 + 0.0001);
                                a = new DBPoint(
                                        (int) (1 - t) * x0 + t * x1,
                                        (int) (1 - t) * y0 + t * y1
                                );

                                x2 = imageSize.width;
                                t = (x0 - x2) / (x0 - x1 + 0.0001);
                                b = new DBPoint(
                                        (int)(1 - t) * x0 + t * x1,
                                        (int)(1 - t) * y0 + t * y1
                                );

                                poly1.add(new DBPoint(0, 0));
                                poly1.add(new DBPoint(imageSize.width, 0));
                                poly1.add(a);
                                poly1.add(b);

                                poly2.add(a);
                                poly2.add(b);
                                poly2.add(new DBPoint(0, imageSize.height));
                                poly2.add(new DBPoint(imageSize.width, imageSize.height));

                                vertical = false;
                            }

                            sortPoints(poly1);
                            s1 = polyScore(poly1, finalPoints, centroid, alpha, beta);

                            sortPoints(poly2);
                            s2 = polyScore(poly2, finalPoints, centroid, alpha, beta);

                            if (s1 == 0 && s2 == 0) {
                                continue;
                            }

                            if (vertical) {
                                dbSegmentsVertical.add(new DBSegment(a, b));
                            }
                            else {
                                dbSegmentsHorizontal.add(new DBSegment(a, b));
                            }
                        }
                    }
                }
            }
            */

            removeDuplicates(dbSegmentsVertical);
            removeDuplicates(dbSegmentsHorizontal);

            List<Pair<Double, List<DBPoint>>> score = new LinkedList<>();


            for (Pair<DBSegment, DBSegment> v : combinations(dbSegmentsVertical)) {
                for (Pair<DBSegment, DBSegment> h : combinations(dbSegmentsHorizontal)) {
                    List<DBPoint> poly = new LinkedList<>();
                    List<DBPoint> outPoints = new LinkedList<>();

                    poly.add(v.first.intersection(v.second));
                    poly.add(v.first.intersection(h.first));
                    poly.add(v.first.intersection(h.second));
                    poly.add(v.second.intersection(h.first));
                    poly.add(v.second.intersection(h.second));
                    poly.add(h.first.intersection(h.second));

                    for (DBPoint point : poly) {
                        if (point.checkCorrectness(imageSize)) {
                            point.normalize();
                        }
                        else {
                            outPoints.add(point);
                        }
                    }
                    poly.removeAll(outPoints);


                    if (poly.size() != 4) {
                        continue;
                    }

                    sortPoints(poly);

                    if (!Imgproc.isContourConvex(dbPointListToMatOfPoint(poly))) {
                        continue;
                    }

                    score.add(new Pair(-polyScore(poly, finalPoints, centroid, alpha, beta), poly));
                }
            }

            if (score.size() > 0) {
                Collections.sort(score, (o1, o2) -> o1.first.compareTo(o2.first));

                List<DBPoint> innerPoints = score.get(0).second;
                DBPoint[] cropPoints = orderPoints(innerPoints);
                DBPoint[] padPoints = padCrop(cropPoints);

                List<double[]> result = new ArrayList<>();
                for (DBPoint padPoint : padPoints) {
                    result.add(padPoint.toRaw());
                }

                return result;
            }
        }
        catch (DBException e) {
            // TODO Error
        }

        return null;
    }

}
