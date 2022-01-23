package com.angmolin.livechess2fen;

import java.util.ArrayList;
import java.util.List;

public class Fen {

    private static final int chessboardSize = 8;
    private static final char pieceTypes[] = { 'r', 'n', 'b', 'q', 'k', 'p', 'P', 'R', 'N', 'B', 'Q', 'K', '_' };

    public enum A1Pos {
        BottomLeft,
        BottomRight,
        TopLeft,
        TopRight
    }

    public static char[][] fenToBoard(String fen) {
        char result[][] = new char[chessboardSize][chessboardSize];

        String[] rows = fen.split("/");
        for (int i = 0; i < chessboardSize; i++) {
            int offset = 0;

            for (int j = 0; j + offset < chessboardSize; j++) {
                char c = rows[i].charAt(j);

                if (c >= 49 && c <= 57) { // Es un numero (1 ~ 9)
                    int n = c - 48;

                    for (int k = 0; k < n; k++) {
                        result[i][j + offset + k] = '_';
                    }
                    offset += n - 1;
                }
                else {
                    result[i][j + offset] = c;
                }
            }
        }

        return result;
    }

    public static String boardToFen(char[][] board) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < chessboardSize; i++) {
            boolean prevEmpty = false;
            int empty = 0;

            for (int j = 0; j < chessboardSize; j++) {
                char c = board[i][j];

                if (c == '_') {
                    empty += 1;
                    prevEmpty = true;
                }
                else {
                    if (prevEmpty) {
                        result.append(empty);

                        empty = 0;
                        prevEmpty = false;
                    }

                    result.append(c);
                }
            }

            if (prevEmpty) {
                result.append(empty);
            }

            if (i < chessboardSize - 1) {
                result.append('/');
            }
        }

        return result.toString();
    }

    public static String boardToFen(Character[] board) {
        char board2d[][] = new char[chessboardSize][chessboardSize];

        for (int i = 0; i < board.length; i++) {
            board2d[i / 8][i % 8] = board[i];
        }

        return boardToFen(board2d);
    }

    public static float[][][] listToBoard(float[][] list, A1Pos a1Pos) {
        // list[64][13]:float
        //
        if (list.length != 64) {
            throw new IllegalArgumentException("List must have 64 entries");
        }

        float result[][][] = new float[chessboardSize][chessboardSize][list[0].length];

        for (int i = 0; i < list.length; i++) {
            result[i / 8][i % 8] = list[i];
        }

        return rotateBoardImageToFen(result, a1Pos);
    }

    public static float[][] boardToList(float[][][] board) {
        float[][] result = new float[board.length * board[0].length][board[0][0].length];

        for (int i = 0; i < chessboardSize; i++) {
            for (int j = 0; j < chessboardSize; j++) {
                result[i * chessboardSize + j] = board[i][j];
            }
        }

        return result;
    }

    public static List<Character> boardToList(char[][] board) {
        List<Character> result = new ArrayList<>();

        for (int i = 0; i < chessboardSize; i++) {
            for (int j = 0; j < chessboardSize; j++) {
                result.add(board[i][j]);
            }
        }

        return result;
    }

    public static boolean isWhiteSquare(int listPos) {
        if (listPos < 0 || listPos > 63) {
            throw new IllegalArgumentException("listPos should be into the range 0-63");
        }

        if (listPos % 16 < 8) { // Odd rows
            return listPos % 2 == 0;
        }
        else { // Even rows
            return listPos % 2 == 1;
        }
    }

    public static float[][][] rotateBoardFenToImage(float[][][] board, A1Pos a1Pos) {
        float result[][][];

        /* Codigo de test de python para la conversion
        board = [
            ['r', 'n', 'b', 'q', 'k', 'b', 'n', 'r'],
            ['p', 'p', 'p', 'p', 'p', 'p', 'p', 'p'],
            ['_', '_', '_', '_', '_', '_', '_', '_'],
            ['_', '_', '_', '_', '_', '_', '_', '_'],
            ['_', '_', '_', '_', '_', '_', '_', '_'],
            ['_', '_', '_', '_', '_', '_', '_', '_'],
            ['P', 'P', 'P', 'P', 'P', 'P', 'P', 'P'],
            ['R', 'N', 'B', 'Q', 'K', 'B', 'N', 'R']
        ];

        tmp = list(map(list, zip(*board[::-1])))
        print(list(map(list, zip(*tmp[::-1]))));
         */

        if (a1Pos == A1Pos.BottomLeft) {
            result = board;
        }
        else {
            result = new float[board.length][board[0].length][board[0][0].length];

            for (int i = 0; i < chessboardSize; i++) {
                for (int j = 0; j < chessboardSize; j++) {
                    if (a1Pos == A1Pos.BottomRight) {
                        result[i][j] = board[j][i];
                    }
                    else if (a1Pos == A1Pos.TopLeft) {
                        result[i][j] = board[1 - chessboardSize - j][i];
                    }
                    else if (a1Pos == A1Pos.TopRight) {
                        result[i][j] = board[1 - chessboardSize - i][1 - chessboardSize - j];
                    }
                }
            }
        }

        return result;
    }

    public static float[][][] rotateBoardImageToFen(float[][][] board, A1Pos a1Pos) {
        if (a1Pos == A1Pos.BottomRight) {
            a1Pos = A1Pos.TopLeft;
        }
        else if (a1Pos == A1Pos.TopLeft) {
            a1Pos = A1Pos.BottomRight;
        }

        return rotateBoardFenToImage(board, a1Pos);
    }

    public static int compareFen(String fen1, String fen2) {
        int result = 0;

        char[][] board1 = fenToBoard(fen1);
        char[][] board2 = fenToBoard(fen2);

        for (int i = 0; i < chessboardSize; i++) {
            for (int j = 0; j < chessboardSize; j++) {
                if (board1[i][j] != board2[i][j]) {
                    result++;
                }
            }
        }

        return result;
    }

}
