package com.angmolin.livechess2fen.chessboard.algorithm.dbscan.types;

import com.angmolin.livechess2fen.chessboard.algorithm.bentleyottmann.types.BOPoint;
import com.angmolin.livechess2fen.chessboard.algorithm.bentleyottmann.types.BOSegment;

import org.opencv.core.Point;

import java.util.Objects;

public class DBSegment {

    public final DBPoint p1;
    public final DBPoint p2;
    public final double length;
    public final double deltaX;
    public final double deltaY;

    public DBSegment(DBPoint p1, DBPoint p2) {
        this.p1 = p1;
        this.p2 = p2;

        this.length = Math.sqrt(
                Math.pow(second().x - first().x, 2) + Math.pow(second().y - first().y, 2)
        );
        this.deltaX = Math.abs(p2.x - p1.x);
        this.deltaY = Math.abs(p2.y - p1.y);
    }

    public DBPoint first() {
        if(p1.x <= p2.x) {
            return p1;
        }
        else {
            return p2;
        }
    }

    public DBPoint second() {
        if(p1.x <= p2.x) {
            return p2;
        }
        else {
            return p1;
        }
    }

    public double distanceTo(DBPoint point) {
        return Math.abs(
                (this.second().x - this.first().x) * (this.first().y - point.y) -
                        (this.second().y - this.first().y) * (this.first().x - point.x)
        ) / length;
    }

    public DBPoint intersection(DBSegment that) {
        DBPoint diffX = new DBPoint(
                this.first().x - this.second().x,
                that.first().x - that.second().x
        );
        DBPoint diffY = new DBPoint(
                this.first().y - this.second().y,
                that.first().y - that.second().y
        );

        double div = det(diffX, diffY);

        if (div == 0) {
            return new DBPoint(-1, -1);
        }

        DBPoint d = new DBPoint(
                det(this.first(), this.second()),
                det(that.first(), that.second())
        );

        double x = det(d, diffX) / div;
        double y = det(d, diffY) / div;

        return new DBPoint(x, y);
    }

    public double[] toRaw() {
        return new double[] { p1.x, p1.y, p2.x, p2.y };
    }

    private static double det(DBPoint a, DBPoint b) {
        return a.x * b.y - a.y * b.x;
    }

    public static DBSegment fromRawData(double[] raw) {
        return new DBSegment(
                new DBPoint(raw[0], raw[1]),
                new DBPoint(raw[2], raw[3])
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DBSegment segment = (DBSegment) o;
        return Double.compare(segment.length, length) == 0 &&
                Double.compare(segment.deltaX, deltaX) == 0 &&
                Double.compare(segment.deltaY, deltaY) == 0 &&
                Objects.equals(p1, segment.p1) &&
                Objects.equals(p2, segment.p2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(p1, p2, length, deltaX, deltaY);
    }
}
