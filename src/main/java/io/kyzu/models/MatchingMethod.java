package io.kyzu.models;

import org.opencv.imgproc.Imgproc;

/**
 * List of template matching methods for the OpenCV algorithm.
 * We refer to use the "_NORMED" version of the methods.
 */
public enum MatchingMethod {

    /**
     * Sum of normalized squared differences between pixel values.
     */
    MM_SQDIFF_NORMED(Imgproc.TM_SQDIFF_NORMED),

    /**
     * Normalized cross correlation.
     */
    MM_CCORR_NORMED(Imgproc.TM_CCORR_NORMED),

    /**
     * Normalized correlation coefficient.
     */
    MM_CCOEFF_NORMED(Imgproc.TM_CCOEFF_NORMED),

    /**
     * Sum of squared differences between pixel values.
     */
    MM_SQDIFF(Imgproc.TM_SQDIFF),

    /**
     * Cross correlation.
     */
    MM_CCORR(Imgproc.TM_CCORR),

    /**
     * Correlation coefficient.
     */
    MM_CCOEFF(Imgproc.TM_CCOEFF);


    private int templateMat;

    MatchingMethod(int templateMat) {
        this.templateMat = templateMat;
    }

    public int getTemplateMat() {
        return templateMat;
    }

    @Override
    public String toString() {
        return "MatchingMethod {" +
               "templateMat = " + templateMat +
               '}';
    }
}
