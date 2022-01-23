package com.angmolin.livechess2fen.chessboard.algorithm.bentleyottmann.types;

import java.util.ArrayList;
import java.util.Arrays;

public class BOEvent {

    private BOPoint point;
    private ArrayList<BOSegment> segments;
    private double value;
    private int type;

    public BOEvent(BOPoint point, BOSegment segment, int type) {
        this.point = point;
        this.segments = new ArrayList<>(Arrays.asList(segment));
        this.value = point.x;
        this.type = type;
    }

    public BOEvent(BOPoint point, ArrayList<BOSegment> segment, int type) {
        this.point = point;
        this.segments = segment;
        this.value = point.x;
        this.type = type;
    }

    public BOPoint getPoint() {
        return this.point;
    }

    public ArrayList<BOSegment> getSegments() {
        return this.segments;
    }

    public int getType() {
        return this.type;
    }

    public double getValue() {
        return this.value;
    }
    
}
