package eu.patrickgeiger.lanedetection;

import androidx.appcompat.widget.SwitchCompat;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class CameraListener implements CameraBridgeViewBase.CvCameraViewListener2 {

    public SwitchCompat switch1;
    public SwitchCompat switch2;

    private Mat largeKernel;
    private Mat m;

    /**
     * @param switch1 The first switch
     * @param switch2 The second switch
     */
    public CameraListener(SwitchCompat switch1, SwitchCompat switch2) {
        super();
        this.switch1 = switch1;
        this.switch2 = switch2;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        // Initialise points for perspective transformation
        MatOfPoint2f startPoint = new MatOfPoint2f(
                new Point(width * (0.675), (height / 2d) + 100),
                new Point(width * (0.675), (height / 2d) - 100),
                new Point(width * (6d / 7d), height * (1d / 7d)),
                new Point(width * (6d / 7d), height * (6d / 7d))
        );

        MatOfPoint2f targetPoint = new MatOfPoint2f(
                new Point(0, height * (3d / 4d)),
                new Point(0, height * (1d / 4d)),
                new Point(width, height * (1d / 4d)),
                new Point(width, height * (3d / 4d))
        );
        m = Imgproc.getPerspectiveTransform(startPoint, targetPoint);

        // Define a kernel for morphological transformation
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


    }

    @Override
    public void onCameraViewStopped() {
        // Do nothing on camera view stopped
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat img = inputFrame.rgba();

        // Apply the perspective transformation image
        Mat imgWarped = warpImage(img, false);

        // Apply the color filters to the image
        Mat imgFiltered = colorFilter(imgWarped);

        // Apply the gradient morphological transformation to extract the outline/edges of the lanes
        Imgproc.morphologyEx(imgFiltered, imgFiltered, Imgproc.MORPH_GRADIENT, largeKernel);

        // If the switch2 is enabled the current image should be returned
        if (switch2.isChecked()) {
            return imgFiltered;
        }

        // Apply the Hough Lines Transformation to detect the lanes
        Mat linesP = new Mat();
        Imgproc.HoughLinesP(imgFiltered, linesP, 6, Math.PI / 180, 200, 175, 100);

        // Draw the lines to a new Mat
        Mat lines = Mat.zeros(img.size(), img.type());
        for (int x = 0; x < linesP.rows(); x++) {
            double[] l = linesP.get(x, 0);
            Imgproc.line(lines, new Point(l[0], l[1]), new Point(l[2], l[3]), new Scalar(255, 0, 0), 5, Imgproc.LINE_AA, 0);
        }

        // Reverse the perspective transformation to the lines image
        Imgproc.warpPerspective(lines, lines, m, lines.size(), Imgproc.CV_WARP_INVERSE_MAP);

        // Set the alpha values for the image
        double alphaImg = 0.2d;
        double alphaLines = 1d;

        // If the first switch is enabled the alpha of the camera image is increased
        if (switch1.isChecked()) {
            alphaImg = 0.8d;
        }

        // Combine the camera image with the lines
        Core.addWeighted(img, alphaImg, lines, alphaLines, 1, img);

        // Release the Mat-objects
        linesP.release();
        lines.release();
        imgFiltered.release();
        imgWarped.release();

        // Return the image
        return img;
    }

    /**
     * @param input   The image to which the perspective transformation is to be applied
     * @param reverse If the perspective transformation should be reversed
     * @return The image after the perspective transformation
     */
    public Mat warpImage(Mat input, boolean reverse) {
        Mat tmp = new Mat();
        if (!reverse) {
            Imgproc.warpPerspective(input, tmp, m, input.size());
        } else {
            Imgproc.warpPerspective(input, tmp, m, input.size(), Imgproc.CV_WARP_INVERSE_MAP);
        }
        return tmp;
    }

    /**
     * @param input The image to which the color filters should be applied
     * @return The image after the color filters
     */
    public Mat colorFilter(Mat input) {
        Mat img = new Mat();

        // Convert the color space to HSV
        Imgproc.cvtColor(input, img, Imgproc.COLOR_RGBA2BGR);
        Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2HSV);

        // Filter for the yellow and white lane
        Mat yellowLine = new Mat();
        Mat whiteLine = new Mat();

        Core.inRange(img, new Scalar(15, 80, 160), new Scalar(40, 255, 255), yellowLine);
        Core.inRange(img, new Scalar(0, 0, 230), new Scalar(255, 20, 255), whiteLine);

        // Generate a mask of the both color filter
        Mat mask = new Mat();
        Core.bitwise_or(yellowLine, whiteLine, mask);

        // Apply the mask to the image
        Mat imgMasked = new Mat();
        Core.bitwise_and(img, img, imgMasked, mask);

        // Convert the color space to grayscale
        Imgproc.cvtColor(imgMasked, img, Imgproc.COLOR_BGR2GRAY);

        // Apply a morphological transformation
        Imgproc.morphologyEx(img, img, Imgproc.MORPH_OPEN, largeKernel);

        // Apply a threshold to the image
        Imgproc.threshold(img, img, 50, 255, Imgproc.THRESH_BINARY);

        // Release the Mat-objects
        yellowLine.release();
        whiteLine.release();
        mask.release();
        imgMasked.release();

        // Return the image
        return img;
    }

}
