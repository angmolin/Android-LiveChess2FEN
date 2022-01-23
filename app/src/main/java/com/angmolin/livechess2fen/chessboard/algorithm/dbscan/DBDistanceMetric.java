package com.angmolin.livechess2fen.chessboard.algorithm.dbscan;

import com.angmolin.livechess2fen.chessboard.algorithm.dbscan.exceptions.DBException;

public interface DBDistanceMetric<T> {

    double calculateDistance(T val1, T val2) throws DBException;

}
