package com.angmolin.livechess2fen;

import android.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PiecesInferrer {

    /*
     * Note: variable tops in the original implementation is a List<Pair<float[], Integer>>
     * but in this implementation it has been changed to a List<Pair<Float, Integer>>
     * because it isn't necessary to store all values.
     */

    private static final char[] predictionsDictionary = {
            'B', 'K', 'N', 'P', 'Q', 'R', '_',
            'b', 'k', 'n', 'p', 'q', 'r'
    };

    private static final char[] indexToPiece = {
            'B', 'N', 'P', 'Q', 'R',
            'b', 'n', 'p', 'q', 'r'
    };

    private static final char[] whitePieces = {
            'P', 'B', 'N', 'R', 'K', 'Q'
    };

    private static final char[] blackPieces = {
            'p', 'b', 'n', 'r', 'k', 'q'
    };

    private static List<Pair<Float, Integer>> sorted(List<Pair<float[], Integer>> piecesProbsSort, int index, boolean reverse) {
        // piecesProbsSort[[13]:float, Integer]
        //
        List<Pair<Float, Integer>> pieces = new ArrayList<>(64);
        for (Pair<float[], Integer> prob : piecesProbsSort) {
            pieces.add(new Pair<>(prob.first[index], prob.second));
        }
        pieces.sort((o1, o2) -> o1.first.compareTo(o2.first));

        if (reverse) {
            Collections.reverse(pieces);
        }

        return pieces;
    }

    private static List<List<Pair<Float, Integer>>> sortPiecesList(List<Pair<float[], Integer>> piecesProbsSort) {
        // piecesProbsSort[[13]:float, Integer]
        //
        List<List<Pair<Float, Integer>>> result = new ArrayList<>(indexToPiece.length);

        // White Bishops
        result.add(sorted(piecesProbsSort, 0, true));
        // White Knights
        result.add(sorted(piecesProbsSort, 2, true));
        // White Pawns
        // Pawns can't be in the first or last row
        result.add(sorted(piecesProbsSort.subList(8, 64 - 8), 3, true));
        // White Queens
        result.add(sorted(piecesProbsSort, 4, true));
        // White Rooks
        result.add(sorted(piecesProbsSort, 5, true));

        // Black Bishops
        result.add(sorted(piecesProbsSort, 7, true));
        // Black Knights
        result.add(sorted(piecesProbsSort, 9, true));
        // Black Pawns
        // Pawns can't be in the first or last row
        result.add(sorted(piecesProbsSort.subList(8, 64 - 8), 10, true));
        // Black Queens
        result.add(sorted(piecesProbsSort, 11, true));
        // Black Rooks
        result.add(sorted(piecesProbsSort, 12, true));

        return result;
    }

    private static int maxPiece(List<Pair<Float, Integer>> tops) {
        // Returns the index of the piece with max probability.

        float value = tops.get(0).first; // B
        int idx = 0;

        if (tops.get(1).first > value) { // N
            value = tops.get(1).first;
            idx = 1;
        }

        if (tops.get(2).first > value) { // P
            value = tops.get(2).first;
            idx = 2;
        }

        if (tops.get(3).first > value) { // Q
            value = tops.get(3).first;
            idx = 3;
        }

        if (tops.get(4).first > value) { // R
            value = tops.get(4).first;
            idx = 4;
        }

        if (tops.get(5).first > value) { // b
            value = tops.get(5).first;
            idx = 5;
        }

        if (tops.get(6).first > value) { // n
            value = tops.get(6).first;
            idx = 6;
        }

        if (tops.get(7).first > value) { // p
            value = tops.get(7).first;
            idx = 7;
        }

        if (tops.get(8).first > value) { // q
            value = tops.get(8).first;
            idx = 8;
        }

        if (tops.get(9).first > value) { // r
            // value = tops.get(9).first;
            idx = 9;
        }

        return idx;
    }

    private static boolean checkBishop(int maxIdx, List<Pair<Float, Integer>> tops, boolean[] wBishopSq, boolean[] bBishopSq) {
        // If it's a bishop, check that there is a at most one in
        // each square color
        if (maxIdx == 0) { // White bishop
            if (Fen.isWhiteSquare(tops.get(maxIdx).second)) {
                if (!wBishopSq[0]) {
                    // We're going to set a white bishop in a
                    // white square
                    wBishopSq[0] = true;
                    return true;
                }

                return false;
            }
            if (!wBishopSq[1]) {
                // We're going to set a white bishop in a
                // black square
                wBishopSq[1] = true;
                return true;
            }

            return false;
        }
        else if (maxIdx == 5) { // Black bishop
            if (Fen.isWhiteSquare(tops.get(maxIdx).second)) {
                if (!bBishopSq[0]) {
                    // We're going to set a black bishop in a
                    // white square
                    bBishopSq[0] = true;
                    return true;
                }

                return false;
            }
            if (!bBishopSq[1]) {
                // We're going to set a black bishop in a
                // black square
                bBishopSq[1] = true;
                return true;
            }

            return false;
        }

        // If it's not a bishop there is nothing to check
        return true;
    }

    private static int argMax(float[] squareProbs) {
        int result = -1;
        float max = 0;

        for (int i = 0; i < squareProbs.length; i++) {
            if (squareProbs[i] > max) {
                result = i;
                max = squareProbs[i];
            }
        }

        return result;
    }

    public static Character[] inferChessPieces(float[][] piecesProbs, Fen.A1Pos a1Pos, String previousFen) {
        // piecesProbs[64][13]:float
        //
        piecesProbs = Fen.boardToList(Fen.listToBoard(piecesProbs, a1Pos));

        // Java initializes the arrays to null.
        // Null values represents that no piece is set in that position yet.
        Character[] outPreds = new Character[64];

        int finalMoveSq = -1;
        List<Character> possiblePieces = new ArrayList<>();

        if (previousFen != null && !previousFen.isEmpty()) {
            List<Integer> changedSquaresIdx = changedSquares(previousFen, piecesProbs);
            Movement move = inferredMove(previousFen, piecesProbs, changedSquaresIdx);

            if (move != null) {
                possiblePieces = inferredPiecesFromMove(move);
            }
        }

        // We need to store the original order
        List<Pair<float[], Integer>> piecesProbsSort = new ArrayList<>();
        for (int i = 0; i < piecesProbs.length; i++) {
            piecesProbsSort.add(new Pair<>(piecesProbs[i], i));
        }

        // First choose the kings, there must be one of each color
        Pair<Float, Integer> whiteKing = sorted(piecesProbsSort, 1, true).get(0); // Note: max function just return the maximum of sorted array
        List<Pair<Float, Integer>> blackKingList = sorted(piecesProbsSort, 8, true);

        Pair<Float, Integer> blackKing = blackKingList.get(0);
        if (blackKing.second == whiteKing.second)
            blackKing = blackKingList.get(1);

        outPreds[whiteKing.second] = 'K';
        outPreds[blackKing.second] = 'k';

        int outPredsEmpty = 62; // 64 - 2 because we have already set the kings

        // Then set the blank spaces, the CNN has a very high accuracy
        // detecting these cases
        for (int idx = 0; idx < piecesProbs.length; idx++) {
            float[] piece = piecesProbs[idx];

            if (outPreds[idx] == null) {
                if (isEmptySquare(piece)) {
                    outPreds[idx] = '_';
                    outPredsEmpty--;
                }
            }
        }

        // Save if there is already a bishop in a [white, black] square
        boolean[] wBishopSq = { false, false };
        boolean[] bBishopSq = { false, false };

        // Set the rest of the pieces in the order given by the highest
        // probability of any piece for all the board
        List<List<Pair<Float, Integer>>> piecesLists = sortPiecesList(piecesProbsSort);

        // Index to the highest probability, from each list in piecesLists,
        // that we have not set yet (in the same order than above).
        int[] idx = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

        // Top of each sorted piece list (highest probability of each piece)
        List<Pair<Float, Integer>> tops = new ArrayList<>();
        for (List<Pair<Float, Integer>> pieceList : piecesLists) {
            tops.add(pieceList.get(0));
        }

        // Maximum number of pieces of each type in the same order than tops
        int[] maxPiecesLeft = { 2, 2, 8, 9, 2, 2, 2, 8, 9, 2 };

        while (outPredsEmpty > 0) {
            // Fill in the square in outPreds that has the piece with the
            // maximum probability of all the board
            int maxIdx = maxPiece(tops);
            int square = tops.get(maxIdx).second;

            // If we haven't maxed that piece type and the square is empty
            if (maxPiecesLeft[maxIdx] > 0
                    && outPreds[square] == null
                    && checkBishop(maxIdx, tops, wBishopSq, bBishopSq)) {
                // Fill the square and update counters
                // If we have detected the move previously
                if (square == finalMoveSq && possiblePieces.size() > 0) {
                    // Only fill the square if one of possible pieces
                    if (contains(possiblePieces, indexToPiece[maxIdx])) {
                        outPreds[square] = indexToPiece[maxIdx];
                        outPredsEmpty--;
                        maxPiecesLeft[maxIdx]--;
                    }
                }
                else {
                    outPreds[square] = indexToPiece[maxIdx];
                    outPredsEmpty--;
                    maxPiecesLeft[maxIdx]--;
                }
            }

            // In any case we must update the entry in tops with the next
            // highest probability for the piece type we have tried
            idx[maxIdx]++;
            tops.set(maxIdx, piecesLists.get(maxIdx).get(idx[maxIdx]));
        }

        return outPreds;
    }

    private static boolean isEmptySquare(float[] squareProbs) {
        return predictionsDictionary[argMax(squareProbs)] == '_';
    }

    private static boolean isWhitePiece(float[] squareProbs) {
        float white = 0, black = 0;
        for (int i = 0; i < 6; i++) {
            white += squareProbs[i];
        }
        for (int i = 7; i < squareProbs.length; i++) {
            black += squareProbs[i];
        }

        return white >= black;
    }

    private static boolean contains(char[] array, char element) {
        for (char arrayElement : array) {
            if (arrayElement == element) {
                return true;
            }
        }

        return false;
    }

    private static boolean contains(List<Character> list, char element) {
        char[] array = new char[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }

        return contains(array, element);
    }

    private static List<Integer> changedSquares(String previousFen, float[][] currentProbs) {
        // currentProbs[64][13]:float
        //
        List<Character> previousList = Fen.boardToList(Fen.fenToBoard(previousFen));
        List<Integer> changedSquaresIdx = new ArrayList<>();
        for (int idx = 0; idx < previousList.size(); idx++) {
            Character square = previousList.get(idx);

            // Pass the squares in which the previous state (white, black or
            // empty) is the same as the current state
            if (square.equals('_') && isEmptySquare(currentProbs[idx])) {
                continue;
            }
            if (contains(whitePieces, square)
                    && !isEmptySquare(currentProbs[idx])
                    && isWhitePiece(currentProbs[idx])) {
                continue;
            }
            if (contains(blackPieces, square)
                    && !isEmptySquare(currentProbs[idx])
                    && !isWhitePiece(currentProbs[idx])) {
                continue;
            }

            changedSquaresIdx.add(idx);
        }

        return changedSquaresIdx;
    }

    private enum MovementAction {
        WHITE_MOVES,
        WHITE_CAPTURES,
        BLACK_MOVES,
        BLACK_CAPTURES
    }

    private static class Movement {
        private int initialSq;
        private int finalSq;
        private MovementAction action;

        public Movement(int initialSq, int finalSq, MovementAction action) {
            this.initialSq = initialSq;
            this.finalSq = finalSq;
            this.action = action;
        }
    }

    private static Movement inferredMove(String previousFen, float[][] currentProbs, List<Integer> changedSquaresIdx) {
        // currentProbs[64][13]:float
        //
        if (changedSquaresIdx.size() != 2) {
            return null;
        }

        List<Character> previousList = Fen.boardToList(Fen.fenToBoard(previousFen));

        int initialSq, finalSq;

        // Determine which square is the initial and which is the final
        if (isEmptySquare(currentProbs[changedSquaresIdx.get(0)])) {
            initialSq = changedSquaresIdx.get(0);

            if (!isEmptySquare(currentProbs[changedSquaresIdx.get(1)])) {
                finalSq = changedSquaresIdx.get(1);
            }
            else {
                return null;
            }
        }
        else if (isEmptySquare(currentProbs[changedSquaresIdx.get(1)])) {
            initialSq = changedSquaresIdx.get(1);

            if (!isEmptySquare(currentProbs[changedSquaresIdx.get(0)])) {
                finalSq = changedSquaresIdx.get(0);
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }

        // We know that in the previous board, the initial square was
        // occupied (now it is empty) and in the current board the final
        // square is occupied
        if (contains(whitePieces, previousList.get(initialSq))) {
            // The initial square is a white piece
            if (previousList.get(finalSq).equals('_')) {
                if (isWhitePiece(currentProbs[finalSq])) {
                    return new Movement(initialSq, finalSq, MovementAction.WHITE_MOVES);
                }
                else {
                    // White piece converts into a black piece?
                    return null;
                }
            }
            else if (contains(blackPieces, previousList.get(finalSq))) {
                if (isWhitePiece(currentProbs[finalSq])) {
                    return new Movement(initialSq, finalSq, MovementAction.WHITE_CAPTURES);
                }
                else {
                    // White piece converts into a black piece?
                    return null;
                }
            }
            else {
                // White piece captures white piece?
                return null;
            }
        }
        else {
            // The initial square is a black piece
            if (previousList.get(finalSq).equals('_')) {
                if (!isWhitePiece(currentProbs[finalSq])) {
                    return new Movement(initialSq, finalSq, MovementAction.BLACK_MOVES);
                }
                else {
                    // Black piece converts into a white piece?
                    return null;
                }
            }
            else if (contains(whitePieces, previousList.get(finalSq))) {
                if (!isWhitePiece(currentProbs[finalSq])) {
                    return new Movement(initialSq, finalSq, MovementAction.BLACK_CAPTURES);
                }
                else {
                    // Black piece converts into a white piece?
                    return null;
                }
            }
            else {
                // Black piece captures black piece?
                return null;
            }
        }
    }

    private static boolean isKingMove(int initialSq[], int finalSq[]) {
        // At most distance one in any direction
        return Math.abs(initialSq[0] - finalSq[0]) <= 1
                && Math.abs(initialSq[1] - finalSq[1]) <= 1;
    }

    private static boolean isRookMove(int initialSq[], int finalSq[]) {
        // Same row or column
        return initialSq[0] == finalSq[0]
                || initialSq[1] == finalSq[1];
    }

    private static boolean isBishopMove(int initialSq[], int finalSq[]) {
        // Same diagonal
        return initialSq[0] - initialSq[1] == finalSq[0] - finalSq[1] // Parallel to main diagonal
                || initialSq[0] + initialSq[1] == finalSq[0] + finalSq[1]; // Parallel to secondary diagonal
    }

    private static boolean isKnightMove(int initialSq[], int finalSq[]) {
        // L shape
        int rowD = Math.abs(initialSq[0] - finalSq[0]);
        int colD = Math.abs(initialSq[1] - finalSq[1]);

        return (rowD == 1 && colD == 2)
                || (rowD == 2 && colD == 1);
    }

    private static boolean isPawnMove(int initialSq[], int finalSq[], boolean capturing, boolean white) {
        // Moves forward in the same column at distance one (or two if it
        // hasn't moved yet) and captures forward diagonally at distance one.

        if (white) {
            if (capturing) {
                return initialSq[0] - finalSq[0] == 1
                        && Math.abs(initialSq[1] - finalSq[1]) == 1;
            }
            else {
                return initialSq[1] == finalSq[1]
                        && (initialSq[0] - finalSq[0] == 1
                        || (initialSq[0] - finalSq[0] == 2
                        && initialSq[0] == 6));
            }
        }
        else {
            if (capturing) {
                return initialSq[0] - finalSq[0] == -1
                        && Math.abs(initialSq[1] - finalSq[1]) == 1;
            }
            else {
                return initialSq[1] == finalSq[1]
                        && (initialSq[0] - finalSq[0] == -1
                        || (initialSq[0] - finalSq[0] == -2
                        && initialSq[0] == 1));
            }
        }
    }

    private static List<Character> inferredPiecesFromMove(Movement movement) {
        int[] initialSq = { movement.initialSq / 8, movement.initialSq % 8 }; // row, column
        int[] finalSq = { movement.finalSq / 8, movement.finalSq % 8 }; // row, column

        boolean capturing = movement.action == MovementAction.WHITE_CAPTURES ||
                    movement.action == MovementAction.BLACK_CAPTURES;
        boolean white = movement.action == MovementAction.WHITE_MOVES ||
                    movement.action == MovementAction.WHITE_CAPTURES;

        List<Character> possiblePieces = new ArrayList<>(); // There can't be duplicates

        if (white) {
            if (isPawnMove(initialSq, finalSq, capturing, white)) {
                if (finalSq[0] == 0) {
                    // If the move ends in the last row, promotions apply,
                    // so the result no longer is a pawn. This move also
                    // corresponds with a king, so the result can be all
                    // pieces except for the pawn. In this case we don't need
                    // to check the rest of the pieces.
                    Collections.addAll(possiblePieces, 'K', 'R', 'B', 'Q', 'N');
                    return possiblePieces;
                }
                possiblePieces.add('P');
            }
            if (isKingMove(initialSq, finalSq)) {
                possiblePieces.add('K');
            }
            if (isRookMove(initialSq, finalSq)) {
                possiblePieces.add('R');
                possiblePieces.add('Q');
            }
            if (isBishopMove(initialSq, finalSq)) {
                possiblePieces.add('B');
                // Bishop and rook moves are exclusive, so Q is not in
                // possible pieces
                possiblePieces.add('Q');
            }
            if (isKnightMove(initialSq, finalSq)) {
                possiblePieces.add('N');
            }
        }
        else {
            if (isPawnMove(initialSq, finalSq, capturing, white)) {
                if (finalSq[0] == 7) {
                    // If the move ends in the last row, promotions apply,
                    // so the result no longer is a pawn. This move also
                    // corresponds with a king, so the result can be all
                    // pieces except for the pawn. In this case we don't need
                    // to check the rest of the pieces.
                    Collections.addAll(possiblePieces, 'k', 'r', 'b', 'q', 'n');
                    return possiblePieces;
                }
                possiblePieces.add('p');
            }
            if (isKingMove(initialSq, finalSq)) {
                possiblePieces.add('k');
            }
            if (isRookMove(initialSq, finalSq)) {
                possiblePieces.add('r');
                possiblePieces.add('q');
            }
            if (isBishopMove(initialSq, finalSq)) {
                possiblePieces.add('b');
                possiblePieces.add('q');
            }
            if (isKnightMove(initialSq, finalSq)) {
                possiblePieces.add('n');
            }
        }

        return possiblePieces;
    }

}
