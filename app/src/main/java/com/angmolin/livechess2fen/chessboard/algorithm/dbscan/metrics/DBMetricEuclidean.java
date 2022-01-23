package com.angmolin.livechess2fen.chessboard.algorithm.dbscan.metrics;

import com.angmolin.livechess2fen.chessboard.algorithm.dbscan.DBDistanceMetric;
import com.angmolin.livechess2fen.chessboard.algorithm.dbscan.exceptions.DBException;
import com.angmolin.livechess2fen.chessboard.algorithm.dbscan.types.DBPoint;

public class DBMetricEuclidean implements DBDistanceMetric<DBPoint> {

    @Override
    public double calculateDistance(DBPoint val1, DBPoint val2) throws DBException {
        return (val1.x - val2.x) + (val1.y - val2.y);
    }

}
