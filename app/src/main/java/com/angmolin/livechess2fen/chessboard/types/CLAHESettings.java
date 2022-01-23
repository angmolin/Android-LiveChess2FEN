package com.angmolin.livechess2fen.chessboard.types;

import org.opencv.core.Size;

public class CLAHESettings {

    public int limit = 0;
    public Size grid = null;
    public int iterations = 0;

    public CLAHESettings(int limit, Size grid, int iterations) {
        this.limit = limit;
        this.grid = grid;
        this.iterations = iterations;
    }
}
