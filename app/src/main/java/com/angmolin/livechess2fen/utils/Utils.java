package com.angmolin.livechess2fen.utils;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

public class Utils {

    public static Bitmap previewBitmap(@NonNull Mat mat) {
        if (mat != null && mat.width() > 0 && mat.height() > 0) {
            Bitmap previewBitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.RGB_565);
            org.opencv.android.Utils.matToBitmap(mat, previewBitmap);

            return previewBitmap;
        }
        else {
            return null;
        }
    }

}
