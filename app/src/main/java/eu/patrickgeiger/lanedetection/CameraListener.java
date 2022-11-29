package eu.patrickgeiger.lanedetection;

import android.util.Log;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class CameraListener implements CameraBridgeViewBase.CvCameraViewListener2 {

    private MatOfPoint2f startPoint;
    private MatOfPoint2f targetPoint;

    private Mat largeKernel;
    private Mat m;

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.i("Resolution   ", "width" + width + "; height " + height);
        startPoint = new MatOfPoint2f(
                new Point(width * (0.675), (height / 2d) + 100),
                new Point(width * (0.675), (height / 2d) - 100),
                new Point(width * (6d / 7d), height * (1d / 7d)),
                new Point(width * (6d / 7d), height * (6d / 7d))
        );

        targetPoint = new MatOfPoint2f(
                new Point(0, height * (3d / 4d)),
                new Point(0, height * (1d / 4d)),
                new Point(width, height * (1d / 4d)),
                new Point(width, height * (3d / 4d))
        );
        largeKernel = new Mat();
        largeKernel.put(0, 0, 0);
        largeKernel.put(0, 1, 1);
        largeKernel.put(0, 2, 1);
        largeKernel.put(0, 3, 0);

        largeKernel.put(1, 0, 1);
        largeKernel.put(1, 1, 1);
        largeKernel.put(1, 2, 1);
        largeKernel.put(1, 3, 1);

        largeKernel.put(2, 0, 1);
        largeKernel.put(2, 1, 1);
        largeKernel.put(2, 2, 1);
        largeKernel.put(2, 3, 1);

        largeKernel.put(3, 0, 0);
        largeKernel.put(3, 1, 1);
        largeKernel.put(3, 2, 1);
        largeKernel.put(3, 3, 0);


        m = Imgproc.getPerspectiveTransform(startPoint, targetPoint);
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat img = inputFrame.rgba();

        Mat img_warped = warpImage(img, false);

        Mat img_filtered = colorFilter(img_warped);

        Imgproc.morphologyEx(img_filtered, img_filtered, Imgproc.MORPH_GRADIENT, largeKernel);

        // Return filtered image and the unused operations
        if (MainActivity.switch2Value) {
            return img_filtered;
        }

        Mat linesP = new Mat();
        Imgproc.HoughLinesP(img_filtered, linesP, 6, Math.PI / 180, 160, 40, 25);

        Mat lines = Mat.zeros(img.size(), img.type());
        for (int x = 0; x < linesP.rows(); x++) {
            double[] l = linesP.get(x, 0);
            Imgproc.line(lines, new Point(l[0], l[1]), new Point(l[2], l[3]), new Scalar(255, 0, 0), 10, Imgproc.LINE_AA, 0);
        }

        Imgproc.warpPerspective(lines, lines, m, lines.size(), Imgproc.CV_WARP_INVERSE_MAP);

        double alpha_img = 0.2d;
        double alpha_lines = 1d;

        if (MainActivity.switch1Value) {
            alpha_img = 0.8d;
        }

        Core.addWeighted(img, alpha_img, lines, alpha_lines, 1, img);

        linesP.release();
        lines.release();
        img_filtered.release();
        img_warped.release();

        return img;
    }

    public Mat warpImage(Mat input, boolean reverse) {
        Mat tmp = new Mat();
        if (!reverse) {
            Imgproc.warpPerspective(input, tmp, m, input.size());
        } else {
            Imgproc.warpPerspective(input, tmp, m, input.size(), Imgproc.CV_WARP_INVERSE_MAP);
        }
        return tmp;
    }

    public Mat colorFilter(Mat input) {
        Mat img = new Mat();
        Imgproc.cvtColor(input, img, Imgproc.COLOR_RGBA2BGR);
        Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2HSV);

        Mat yellow_line = new Mat();
        Mat white_line = new Mat();

        Core.inRange(img, new Scalar(15, 80, 160), new Scalar(40, 255, 255), yellow_line);
        Core.inRange(img, new Scalar(0, 0, 225), new Scalar(255, 20, 255), white_line);

        Mat mask = new Mat();
        Core.bitwise_or(yellow_line, white_line, mask);

        Mat img_masked = new Mat();
        Core.bitwise_and(img, img, img_masked, mask);
        Imgproc.cvtColor(img_masked, img_masked, Imgproc.COLOR_BGR2GRAY);

        Imgproc.morphologyEx(img_masked, img, Imgproc.MORPH_CLOSE, largeKernel);

        yellow_line.release();
        white_line.release();
        mask.release();
        img_masked.release();
        return img;
    }

}
