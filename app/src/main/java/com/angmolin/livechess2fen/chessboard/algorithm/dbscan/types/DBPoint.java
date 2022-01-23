package com.angmolin.livechess2fen.chessboard.algorithm.dbscan.types;

import org.opencv.core.Size;

import java.util.Objects;

public class DBPoint {

    public double x;
    public double y;
    public Double latLng;

    public DBPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void normalize() {
        this.x = (int) this.x;
        this.y = (int) this.y;
    }

    public boolean checkCorrectness(Size shape) {
        return this.x >= 0 && this.x <= shape.width &&
                this.y >= 0 && this.y <= shape.height;
    }

    public static DBPoint fromRaw(double[] raw) {
        return new DBPoint(raw[0], raw[1]);
    }

    public double[] toRaw() {
        return new double[] { x, y };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DBPoint dbPoint = (DBPoint) o;
        return Double.compare(dbPoint.x, x) == 0 &&
                Double.compare(dbPoint.y, y) == 0 &&
                Objects.equals(latLng, dbPoint.latLng);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, latLng);
    }
}
