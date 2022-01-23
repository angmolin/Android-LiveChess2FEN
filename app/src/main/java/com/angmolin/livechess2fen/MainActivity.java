package com.angmolin.livechess2fen;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.angmolin.livechess2fen.chessboard.ChessboardDetector;
import com.angmolin.livechess2fen.chessboard.ChessboardDrawer;
import com.angmolin.livechess2fen.classifier.ImageClassifier;
import com.angmolin.livechess2fen.classifier.TFImageClassifier;
import com.angmolin.livechess2fen.types.ImageObject;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.angmolin.livechess2fen.utils.Utils.previewBitmap;

public class MainActivity extends AppCompatActivity implements CvCameraViewListener2, ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "LiveChess2FEN";

    private static final int PERMISSION_REQUESTS = 1;

    private Pair<List<double[]>, List<double[]>> lastFourPointsCornerPoints;
    private String lastFEN;

    private boolean benchmarkMode = false;
    private boolean viewChessboardDraw = true;

    private TextView cameraViewText;
    private JavaCameraView cameraBridgeViewBase;
    private TextView imageViewText;
    private ImageView imageView;

    private ChessboardDetector chessboardDetector;

    private ImageClassifier lapsClassifier;
    private int lapsClassifierCores;
    private ImageClassifier piecesClassifier;
    private int piecesClassifierCores;

    private String lapsCNN;
    private String piecesCNN;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    cameraBridgeViewBase.enableView();
                    cameraBridgeViewBase.disableFpsMeter();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(
                TAG,
                "Application created! Let's see how long it takes to explode. (I'm joking ...or not)"
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (allPermissionsGranted()) {
            initializeUIComponents();
        } else {
            getRuntimePermissions();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                break;
        }
        return true;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        lapsCNN = sharedPreferences.getString("laps_cnn", "laps.tflite");
        piecesCNN = sharedPreferences.getString("pieces_cnn", "MobileNetV2.tflite");
        benchmarkMode = sharedPreferences.getBoolean("benchmark", false);
        viewChessboardDraw = sharedPreferences.getBoolean("draw", true);

        initializeOpenCV();
        initializeImageClassifiers();
        initializeChessboardDetector();
    }

    private void initializeUIComponents() {
        cameraViewText = findViewById(R.id.cameraViewText);

        cameraBridgeViewBase = findViewById(R.id.cameraView);

        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.enableFpsMeter();
        cameraBridgeViewBase.setCvCameraViewListener(this);

        imageViewText = findViewById(R.id.imageViewText);
        imageView = findViewById(R.id.imageView);
    }

    private void initializeOpenCV() {
        if (!OpenCVLoader.initDebug()) {
            Log.d(
                    TAG,
                    "OpenCV library not found. It'd be better if you include in future compilations while so I'm going to use OpenCV Manager."
            );

            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        }
        else {
            Log.d(
                    TAG,
                    "OpenCV library found. Good job boy! ;)"
            );

            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private void initializeImageClassifiers() {
        try {
            this.lapsClassifierCores = Math.max(Runtime.getRuntime().availableProcessors() / 2, 1);
            this.lapsClassifier = new TFImageClassifier(
                    getAssets(),
                    "tflite_models/laps/" + lapsCNN,
                    0.5f,
                    1,
                    new Size(21, 21),
                    1,
                    2,
                    this.lapsClassifierCores
            );

            this.piecesClassifierCores = Math.max(Runtime.getRuntime().availableProcessors() / 4, 1);
            this.piecesClassifier = new TFImageClassifier(
                    getAssets(),
                    "tflite_models/pieces/" + piecesCNN,
                    0.5f,
                    1,
                    new Size(224, 224),
                    3,
                    13,
                    this.piecesClassifierCores
            );
        }
        catch (IOException e) {
            // TODO Error
        }
    }

    private void initializeChessboardDetector() {
        this.chessboardDetector = new ChessboardDetector(lapsClassifier);
    }
    
    @Override
    public void onPause()
    {
        super.onPause();
        if (cameraBridgeViewBase != null)
            cameraBridgeViewBase.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (cameraBridgeViewBase != null)
            cameraBridgeViewBase.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        // When the camera view is started this method is called
    }

    public void onCameraViewStopped() {
        // When the camera view is stopped this method is called
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat inputMat = inputFrame.rgba();
        Mat result;

        StringBuilder Stats = new StringBuilder();
        long startTime, endTime;
        long startChessboardDetectorDetect, endChessboardDetectorDetect;
        long startBoardSplitterSplitSquaresFromBoardImage, endBoardSplitterSplitSquaresFromBoardImage;
        long startPiecesClassifierClassifyAndGetResult, endPiecesClassifierClassifyAndGetResult;
        long startPiecesInferrerInferChessPieces, endPiecesInferrerInferChessPieces;
        double fps;

        startTime = SystemClock.uptimeMillis();
        startChessboardDetectorDetect = SystemClock.uptimeMillis();

        try {
            // Step 1: Chessboard detect
            ImageObject imageObject = chessboardDetector.detect(inputMat, lastFourPointsCornerPoints != null ? lastFourPointsCornerPoints.first : null);
            Pair<List<double[]>, List<double[]>> fourPointsCornerPoints = chessboardDetector.computeCorners(imageObject);
            lastFourPointsCornerPoints = fourPointsCornerPoints;
            endChessboardDetectorDetect = SystemClock.uptimeMillis();

            // Step 2: Chessboard square split
            startBoardSplitterSplitSquaresFromBoardImage = SystemClock.uptimeMillis();
            List<Mat> squares = BoardSplitter.splitSquaresFromBoardImage(imageObject.last().original);
            endBoardSplitterSplitSquaresFromBoardImage = SystemClock.uptimeMillis();

            // Step 3: Convolutional neural network
            startPiecesClassifierClassifyAndGetResult = SystemClock.uptimeMillis();
            float[][] cnnResults = new float[squares.size()][];
            for (int i = 0; i < squares.size(); i++) {
                cnnResults[i] = piecesClassifier.classifyAndGetResult(squares.get(i));
            }
            endPiecesClassifierClassifyAndGetResult = SystemClock.uptimeMillis();

            // Step 4: Piece post-processing
            startPiecesInferrerInferChessPieces = SystemClock.uptimeMillis();
            Character[] boardArray = PiecesInferrer.inferChessPieces(cnnResults, Fen.A1Pos.BottomLeft, null);
            lastFEN = Fen.boardToFen(boardArray);
            endPiecesInferrerInferChessPieces = SystemClock.uptimeMillis();


            if (viewChessboardDraw)
                runOnUiThread(() -> imageView.setImageBitmap(ChessboardDrawer.drawChessboard(getApplicationContext().getResources(), boardArray)));
            else
                runOnUiThread(() -> imageView.setImageBitmap(previewBitmap(imageObject.last().original)));

            for (double[] p : fourPointsCornerPoints.first)
                Imgproc.circle(imageObject.first().original, new Point(p), 10, new Scalar(0, 255, 0), -1);
            for (double[] p : fourPointsCornerPoints.second)
                Imgproc.circle(imageObject.first().original, new Point(p), 10, new Scalar(0, 0, 255), -1);

            Stats.append(String.format("FEN: %s\n\n", lastFEN));
            if (benchmarkMode) {
                Stats.append(String.format("Pieces CNN: %s\n", piecesCNN));
                Stats.append(String.format("Chessboard detect @%d: %d ms\n", lapsClassifierCores, endChessboardDetectorDetect - startChessboardDetectorDetect));
                Stats.append(String.format("Chessboard square split: %d ms\n", endBoardSplitterSplitSquaresFromBoardImage - startBoardSplitterSplitSquaresFromBoardImage));
                Stats.append(String.format("Convolutional neural network @%d: %d ms\n", piecesClassifierCores, endPiecesClassifierClassifyAndGetResult - startPiecesClassifierClassifyAndGetResult));
                Stats.append(String.format("Piece post-processing: %d ms\n", endPiecesInferrerInferChessPieces - startPiecesInferrerInferChessPieces));
            }
            runOnUiThread(() -> imageViewText.setText(Stats));

            result = imageObject.first().original;
        }
        catch (Exception exception) {
            result = inputMat;
        }

        endTime = SystemClock.uptimeMillis();
        fps = 1000.0 / (endTime - startTime);

        runOnUiThread(() -> cameraViewText.setText(String.format("%.2f FPS", fps)));

        return result;
    }

    private String[] getRequiredPermissions() {
        try {
            PackageInfo info = this.getPackageManager().getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);

            String[] ps = info.requestedPermissions;

            if (ps != null && ps.length > 0) {
                return ps;
            }
            else {
                return new String[0];
            }
        }
        catch (Exception e) {
            return new String[0];
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                return false;
            }
        }
        return true;
    }

    private void getRuntimePermissions() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                allNeededPermissions.add(permission);
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.i(
                TAG,
                "Now that I have all the permissions I can take photos of you in the bathroom and...sell them in the dark web. Hahahaha (Although bitcoins are going down)"
        );

        if (allPermissionsGranted()) {
            initializeUIComponents();
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            Log.i(
                    TAG,
                    "Permision granted (Nice for me hahaha):" + permission
            );

            return true;
        }

        Log.i(
                TAG,
                "Permision NOT granted (Are you a close?):" + permission
        );

        return false;
    }
}