package com.angmolin.livechess2fen.chessboard;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

import com.angmolin.livechess2fen.R;

public class ChessboardDrawer {

    private static final int chessboardBitmapOffsetX = 77;
    private static final int chessboardBitmapOffsetY = 77;

    private static Bitmap chessboardBitmap = null;
    private static Bitmap whiteRookBitmap = null;
    private static Bitmap whiteBishopBitmap = null;
    private static Bitmap whiteKnightBitmap = null;
    private static Bitmap whiteKingBitmap = null;
    private static Bitmap whiteQueenBitmap = null;
    private static Bitmap whitePawnBitmap = null;
    private static Bitmap blackRookBitmap = null;
    private static Bitmap blackBishopBitmap = null;
    private static Bitmap blackKnightBitmap = null;
    private static Bitmap blackKingBitmap = null;
    private static Bitmap blackQueenBitmap = null;
    private static Bitmap blackPawnBitmap = null;

    public static Bitmap drawChessboard(Resources resources, Character[] board) {
        if (chessboardBitmap == null)
            chessboardBitmap = BitmapFactory.decodeResource(resources, R.drawable.chessboard);
        if (whiteRookBitmap == null)
            whiteRookBitmap = BitmapFactory.decodeResource(resources, R.drawable.rook_b);
        if (whiteBishopBitmap == null)
            whiteBishopBitmap = BitmapFactory.decodeResource(resources, R.drawable.bishop_b);
        if (whiteKnightBitmap == null)
            whiteKnightBitmap = BitmapFactory.decodeResource(resources, R.drawable.knight_b);
        if (whiteKingBitmap == null)
            whiteKingBitmap = BitmapFactory.decodeResource(resources, R.drawable.king_b);
        if (whiteQueenBitmap == null)
            whiteQueenBitmap = BitmapFactory.decodeResource(resources, R.drawable.queen_b);
        if (whitePawnBitmap == null)
            whitePawnBitmap = BitmapFactory.decodeResource(resources, R.drawable.pawn_b);
        if (blackRookBitmap == null)
            blackRookBitmap = BitmapFactory.decodeResource(resources, R.drawable.rook_n);
        if (blackBishopBitmap == null)
            blackBishopBitmap = BitmapFactory.decodeResource(resources, R.drawable.bishop_n);
        if (blackKnightBitmap == null)
            blackKnightBitmap = BitmapFactory.decodeResource(resources, R.drawable.knight_n);
        if (blackKingBitmap == null)
            blackKingBitmap = BitmapFactory.decodeResource(resources, R.drawable.king_n);
        if (blackQueenBitmap == null)
            blackQueenBitmap = BitmapFactory.decodeResource(resources, R.drawable.queen_n);
        if (blackPawnBitmap == null)
            blackPawnBitmap = BitmapFactory.decodeResource(resources, R.drawable.pawn_n);

        Bitmap chessboard = chessboardBitmap.copy(chessboardBitmap.getConfig(), true);
        Canvas chessboardCanvas = new Canvas(chessboard);

        for (int i = 0; i < board.length; i++) {
            int x = i / 8, y = i % 8;

            Bitmap piece;
            switch (board[i]) {
                case 'R':
                    piece = whiteRookBitmap;
                    break;
                case 'B':
                    piece = whiteBishopBitmap;
                    break;
                case 'N':
                    piece = whiteKnightBitmap;
                    break;
                case 'K':
                    piece = whiteKingBitmap;
                    break;
                case 'Q':
                    piece = whiteQueenBitmap;
                    break;
                case 'P':
                    piece = whitePawnBitmap;
                    break;
                case 'r':
                    piece = blackRookBitmap;
                    break;
                case 'b':
                    piece = blackBishopBitmap;
                    break;
                case 'n':
                    piece = blackKnightBitmap;
                    break;
                case 'k':
                    piece = blackKingBitmap;
                    break;
                case 'q':
                    piece = blackQueenBitmap;
                    break;
                case 'p':
                    piece = blackPawnBitmap;
                    break;
                default:
                    piece = null;
            }

            if (piece != null) {
                chessboardCanvas.drawBitmap(
                        piece,
                        chessboardBitmapOffsetX + piece.getWidth() / 2 + piece.getWidth() * x,
                        chessboardBitmapOffsetY + piece.getHeight() / 2 + piece.getHeight() * y,
                        null
                );
            }
        }

        return chessboard;
    }

}
