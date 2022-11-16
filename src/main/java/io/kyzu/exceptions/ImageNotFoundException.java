package io.kyzu.exceptions;

import java.awt.Rectangle;

import io.kyzu.core.ImageFinderResult;

public class ImageNotFoundException extends RuntimeException {
    private double threshold;
    private Rectangle foundRect;
    private ImageFinderResult bestResult;

    public ImageNotFoundException(String message) {
        super(message);
    }

    public ImageNotFoundException(String message, Rectangle foundRect, Double threshold) {
        super(message);
        this.threshold = threshold;
        this.foundRect = foundRect;
    }

    public ImageNotFoundException(String message, ImageFinderResult bestResult, Rectangle foundRect, Double threshold) {
        super(message);
        this.bestResult = bestResult;
        this.foundRect = foundRect;
        this.threshold = threshold;
    }
}
