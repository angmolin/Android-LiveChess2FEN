package com.angmolin.livechess2fen.chessboard.algorithm.bentleyottmann.types;

public class BOPoint {

    public final double x;
    public final double y;

    public BOPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double distanceTo(BOPoint that) {
        return Math.sqrt(
                Math.pow(that.x - this.x, 2) + Math.pow(that.y - this.y, 2)
        );
    }

    public double[] toRaw() {
        return new double[] { x, y };
    }

}
