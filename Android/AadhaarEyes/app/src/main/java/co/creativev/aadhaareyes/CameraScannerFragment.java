package co.creativev.aadhaareyes;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.cardreader.MainActivity;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

public class CameraScannerFragment extends Fragment implements CameraBridgeViewBase.CvCameraViewListener2 {

    private final LinkedList<Mat> oldFrames;
    private JavaCameraView javaCameraView;
    private Mat screen;
    private MainActivity nativeBridge;
    private int random;
    private int height;
    private int width;
    private int num;
    private int[][] pt;
    private Scalar grayColor;
    private Scalar black;
    private Task task;
    private Handler handler;

    public CameraScannerFragment() {
        nativeBridge = new MainActivity();
        random = new Random().nextInt();
        grayColor = Scalar.all(184);
        black = Scalar.all(0);
        oldFrames = new LinkedList<>();
        handler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        javaCameraView = new JavaCameraView(getActivity(), -1);
        javaCameraView.setCvCameraViewListener(this);
        return javaCameraView;
    }

    public void onManagerConnected() {
        javaCameraView.enableView();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (javaCameraView != null)
            javaCameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (javaCameraView != null)
            javaCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        screen = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat gray = inputFrame.gray().clone();

        RotatedRect max = getBoundingRect(gray);
        Point[] pt = new Point[4];
        max.points(pt);
        Mat out = inputFrame.gray();
        Core.line(out, pt[0], pt[1], grayColor, 3);
        Core.line(out, pt[1], pt[2], grayColor, 3);
        Core.line(out, pt[2], pt[3], grayColor, 3);
        Core.line(out, pt[3], pt[0], grayColor, 3);
        double maxDiff = stabalize(gray);
        Core.putText(out, "Max " + maxDiff, max.center, Core.FONT_HERSHEY_PLAIN, 2, black, 3);

        if (maxDiff < 1) {
            if (task == null || task.finished()) {
                task = new Task(inputFrame.gray(), max);
                task.start();
            } else
                Core.putText(out, "Running", new Point(max.center.x, max.center.y + 40), Core.FONT_HERSHEY_PLAIN, 2, black, 3);
        }
        return out;
    }

    private double stabalize(Mat gray) {
        double maxDiff = 0;
        Mat toBeAdded = gray.clone();
        Imgproc.GaussianBlur(toBeAdded, toBeAdded, new Size(5, 5), 10, 10);
        double totalPixels = toBeAdded.size().area();
        for (Mat oldFrame : oldFrames) {
            maxDiff = Math.max(maxDiff, diff(toBeAdded, oldFrame) / totalPixels);
        }
        oldFrames.add(toBeAdded);
        if (oldFrames.size() > 2)
            oldFrames.remove();
        return maxDiff;
    }

    public double diff(Mat newScreen, Mat oldScreen) {
        Mat temp = new Mat(newScreen.size(), CvType.CV_8UC1);
        Core.subtract(newScreen, oldScreen, temp);
        Core.pow(temp, 2.0, temp);
        return Core.sumElems(temp).val[0];
    }

    private RotatedRect getBoundingRect(Mat gray) {
        Imgproc.Canny(gray, gray, 100, 100);
        Imgproc.dilate(gray, gray, Mat.ones(3, 3, CvType.CV_8UC1));
        Imgproc.threshold(gray, gray, 0, 255, 8);
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(gray,
                contours,
                new Mat(),
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_NONE); // all pixels of each contour
        double area = 0;
        RotatedRect max = null;
        for (MatOfPoint contour : contours) {
            RotatedRect rotatedRect = Imgproc.minAreaRect(new MatOfPoint2f(contour.toArray()));
            if (area < rotatedRect.size.area()) {
                area = rotatedRect.size.area();
                max = rotatedRect;
            }
        }
        return max;
    }

    private class Task extends Thread {
        private final Mat gray;
        private final RotatedRect max;
        private boolean finished;

        public Task(Mat gray, RotatedRect max) {
            this.gray = gray;
            this.max = max;
            finished = false;
        }

        public void run() {
            super.run();
            try {
                sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            finished = true;
        }

        public boolean finished() {
            return finished;
        }
    }
}
