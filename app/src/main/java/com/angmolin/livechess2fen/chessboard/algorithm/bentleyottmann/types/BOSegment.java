package com.angmolin.livechess2fen.chessboard.algorithm.bentleyottmann.types;

public class BOSegment {

    private BOPoint p1;
    private BOPoint p2;
    double value;

    public BOSegment(BOPoint p1, BOPoint p2) {
        this.p1 = p1;
        this.p2 = p2;

        this.calculateValue(this.first().x);
    }

    public BOPoint first() {
        if(p1.x <= p2.x) {
            return p1;
        }
        else {
            return p2;
        }
    }

    public BOPoint second() {
        if(p1.x <= p2.x) {
            return p2;
        }
        else {
            return p1;
        }
    }

    public void calculateValue(double value) {
        double x1 = this.first().x;
        double x2 = this.second().x;
        double y1 = this.first().y;
        double y2 = this.second().y;
        this.value = y1 + (((y2 - y1) / (x2 - x1)) * (value - x1));
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getValue() {
        return this.value;
    }

    public static BOSegment fromRawData(double[] raw) {
        return new BOSegment(
                new BOPoint(raw[0], raw[1]),
                new BOPoint(raw[2], raw[3])
        );
    }
    
}
