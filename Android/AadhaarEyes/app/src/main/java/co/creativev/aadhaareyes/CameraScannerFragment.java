package co.creativev.aadhaareyes;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.GaussianBlur;
import static org.opencv.imgproc.Imgproc.RETR_TREE;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.approxPolyDP;
import static org.opencv.imgproc.Imgproc.arcLength;
import static org.opencv.imgproc.Imgproc.contourArea;
import static org.opencv.imgproc.Imgproc.findContours;
import static org.opencv.imgproc.Imgproc.minAreaRect;
import static org.opencv.imgproc.Imgproc.threshold;

public class CameraScannerFragment extends Fragment implements CameraBridgeViewBase.CvCameraViewListener2 {

    private JavaCameraView javaCameraView;
    private Mat screen;

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
        Mat newFrame = inputFrame.gray();
        Mat rgba = inputFrame.rgba();
        Mat gaussian = new Mat(newFrame.size(), CvType.CV_8UC4);
        GaussianBlur(newFrame, gaussian, new Size(1, 1), 1, 1, 1000);
        threshold(gaussian, gaussian, 120d, 255, THRESH_BINARY);
        Mat hierarchy = new Mat();
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        findContours(gaussian, contours, hierarchy, RETR_TREE, CHAIN_APPROX_SIMPLE);
        Collections.sort(contours, new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint lhs, MatOfPoint rhs) {
                double areaRhs = contourArea(rhs);
                double areaLhs = contourArea(lhs);
                return areaLhs < areaRhs ? -1 : areaLhs > areaRhs ? 1 : 0;
            }
        });
        int rows = newFrame.rows();
        int cols = newFrame.cols();
        screen = gaussian.submat(0, rows, 0, cols);

        Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
        for (MatOfPoint contour : contours) {
            MatOfPoint2f matOfPoint2f = new MatOfPoint2f(contour.toArray());
            double v = arcLength(matOfPoint2f, true);
            MatOfPoint2f curve = new MatOfPoint2f();
            approxPolyDP(matOfPoint2f, curve, 0.02 * v, true);
            RotatedRect rotatedRect = minAreaRect(curve);

            Rect rect = rotatedRect.boundingRect();
//            Core.rectangle(screen, rect.tl(), rect.br(), FACE_RECT_COLOR, 3);
        }

        return screen;
    }
}
