package io.kyzu.core;

import java.awt.Rectangle;

public class ImageFinderResult {

    private double threshold;
    private Rectangle imageLocation;

    public ImageFinderResult(Rectangle imageLocation, double threshold) {
        this.imageLocation = imageLocation;
        this.threshold = threshold;
    }

    public double getThreshold() {
        return threshold;
    }

    public Rectangle getImageLocation() {
        return imageLocation;
    }
}
