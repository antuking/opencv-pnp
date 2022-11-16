package io.kyzu.core;

import static io.kyzu.models.constants.PnPContants.MATCH_THRESHOLD;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.features2d.BFMatcher;
import org.opencv.features2d.ORB;
import org.opencv.features2d.SIFT;

import io.kyzu.exceptions.ImageNotFoundException;
import io.kyzu.interfaces.IKeypointImageFinder;
import io.kyzu.models.MatchingFeature2d;
import nu.pattern.OpenCV;

public class KeypointImageFinder implements IKeypointImageFinder {

    static {
        OpenCV.loadShared();
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
    }

    /**
     * Finds a template image on the screen by keypoints. Throws an exception when the image
     * wasn't found.
     *
     * @param source The source's Mat.
     * @param template The template's Mat.
     * @param detector The matching feature 2d enumeration
     * @return An ImageFinderResult object that stores the rectangle of the
     * found image and desired accuracy.
     */
    @Override
    public ImageFinderResult findImageByKeypoint(Mat source, Mat template, MatchingFeature2d detector) {
        double threshold = MATCH_THRESHOLD;
        Rectangle foundRect = getImageBoundaryByDescriptor(source, template, detector, threshold);
        return new ImageFinderResult(foundRect, threshold);
    }

    /**
     * Finds a template image on the screen by keypoints. Throws an exception when the image
     * wasn't found or the desired accuracy couldn't be met.
     *
     * @param source The source's Mat.
     * @param template The template's Mat.
     * @param detector The matching feature 2d enumeration
     * @param threshold The desired accuracy of the find operation as a
     * number between 0 and 1.
     * @return An ImageFinderResult object that stores the rectangle of the
     * found image and desired accuracy.
     */
    @Override
    public ImageFinderResult findImageByKeypoint(Mat source, Mat template, MatchingFeature2d detector, double threshold) {
        Rectangle foundRect = getImageBoundaryByDescriptor(source, template, detector, threshold);
        return new ImageFinderResult(foundRect, threshold);
    }

    /**
     * Get image boundary by Keypoint
     */
    public Rectangle getImageBoundaryByDescriptor(Mat source, Mat template, MatchingFeature2d detector,
                                                         double threshold) {
        Rectangle rect = new Rectangle();
        MatOfKeyPoint tempMatKp = new MatOfKeyPoint(), srcMatKp = new MatOfKeyPoint();
        Mat descTemp = new Mat(), descSrc = new Mat();
        BFMatcher matcher;
        List<MatOfDMatch> knnMatches = new ArrayList<>();
        try {
            switch (detector) {
                case SIFT:
                    SIFT sift = SIFT.create();
                    sift.detectAndCompute(template, new Mat(), tempMatKp, descTemp);
                    sift.detectAndCompute(source, new Mat(), srcMatKp, descSrc);

                    matcher = BFMatcher.create();
                    matcher.knnMatch(descTemp, descSrc, knnMatches, 2);
                    break;

                case ORB:
                    ORB orb = ORB.create();
                    orb.detectAndCompute(template, new Mat(), tempMatKp, descTemp);
                    orb.detectAndCompute(source, new Mat(), srcMatKp, descSrc);

                    matcher = BFMatcher.create(Core.NORM_L2, true);
                    MatOfDMatch matOfDMatch = new MatOfDMatch();
                    matcher.match(descTemp, descSrc, matOfDMatch);
                    knnMatches.add(matOfDMatch);
                    break;

                default:
                    break;
            }

            List<KeyPoint> tempMatchKps = new ArrayList<>(), srcMatchKps = new ArrayList<>();
            List<KeyPoint> tempKeypoints = tempMatKp.toList(), srcKeypoints = srcMatKp.toList();

            List<MatOfDMatch> goodMatches = new ArrayList<>();

            // get keypoint coordinates of good matches to find homography and remove outliers using ransac
            List<Point> tempPoints = new ArrayList<Point>();
            List<Point> srcPoints = new ArrayList<Point>();

            for (int i = 0; i < knnMatches.size(); i++) {
                DMatch[] matches = knnMatches.get(i).toArray();
                float dist1 = matches[0].distance;
                float dist2 = matches[1].distance;
                if (dist1 < ((float) threshold) * dist2) {
                    goodMatches.add(knnMatches.get(i));     // debug purpose
                    tempMatchKps.add(tempKeypoints.get(matches[0].queryIdx));
                    srcMatchKps.add(srcKeypoints.get(matches[0].trainIdx));
                }
            }

            /**
             * Debug keypoint matching image
             **/
//            Mat debug = new Mat();
//            Features2d.drawMatchesKnn(template, tempMatKp, source, srcMatKp, goodMatches, debug);
//            Imgcodecs.imwrite(detector.toString().toLowerCase() + "_result.png", debug);

            tempPoints = tempMatchKps.stream().map(kp -> kp.pt).collect(Collectors.toList());
            srcPoints = srcMatchKps.stream().map(kp -> kp.pt).collect(Collectors.toList());

            // convertion of data types - there is maybe a more beautiful way
            Mat outputMask = new Mat();
            MatOfPoint2f tempMatOfPoint = new MatOfPoint2f();
            tempMatOfPoint.fromList(tempPoints);
            MatOfPoint2f srcMatOfPoint = new MatOfPoint2f();
            srcMatOfPoint.fromList(srcPoints);

            // Find homography - here just used to perform match filtering with RANSAC, but could be used to e.g. stitch images
            // the smaller the allowed reprojection error (here 15), the more matches are filtered
            Mat homo = Calib3d.findHomography(tempMatOfPoint, srcMatOfPoint, Calib3d.RANSAC, 15, outputMask, 2000, 0.995);

            // Collect the object boundary
            Mat obj_corners = new Mat(4, 1, CvType.CV_32FC2);
            Mat scene_corners = new Mat(4, 1, CvType.CV_32FC2);

            obj_corners.put(0, 0, new double[] {0,0});
            obj_corners.put(1, 0, new double[] {template.cols(),0});
            obj_corners.put(2, 0, new double[] {template.cols(),template.rows()});
            obj_corners.put(3, 0, new double[] {0,template.rows()});

            Core.perspectiveTransform(obj_corners, scene_corners, homo);

            // points of object (debug purpose)
            Point po1 = new Point(obj_corners.get(0, 0));
            Point po2 = new Point(obj_corners.get(1, 0));
            Point po3 = new Point(obj_corners.get(2, 0));
            Point po4 = new Point(obj_corners.get(3, 0));

            // point of object in scene
            Point topLeft = new Point(scene_corners.get(0, 0));
            Point topRight = new Point(scene_corners.get(1, 0));
            Point botRight = new Point(scene_corners.get(2, 0));
            Point botLeft = new Point(scene_corners.get(3, 0));

            rect = new Rectangle((int) topLeft.x, (int) topLeft.y, (int) (topRight.x - topLeft.x), (int) (botLeft.y - topLeft.y));
        } catch (Exception e) {
            throw new ImageNotFoundException("Don't apply keypoint algorithm to detect image by: " + detector.toString());
        }
        return rect;
    }
}
