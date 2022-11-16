package io.kyzu.interfaces;

import org.opencv.core.Mat;

import io.kyzu.core.ImageFinderResult;
import io.kyzu.models.MatchingFeature2d;

public interface IKeypointImageFinder {
    ImageFinderResult findImageByKeypoint(Mat source, Mat template, MatchingFeature2d detector);

    ImageFinderResult findImageByKeypoint(Mat source, Mat template, MatchingFeature2d detector, double threshold);
}
