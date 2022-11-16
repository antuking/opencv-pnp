package io.kyzu.interfaces;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import io.kyzu.core.ImageFinderResult;

import io.appium.java_client.AppiumDriver;

public interface IMatchingImageFinder {
    ImageFinderResult findMatchingImage(AppiumDriver driver, BufferedImage templateImage, double threshold, Rectangle sourceRect);

    ImageFinderResult findAnyMatchingImage(BufferedImage sourceImage, List<BufferedImage> templateImages, double threshold, Rectangle sourceRect);

    ImageFinderResult findAnyMatchingImage(File sourceImage, List<File> templateImages, double threshold, Rectangle sourceRect);

    ImageFinderResult findMatchingImage(BufferedImage sourceImage, BufferedImage templateImage, double threshold, Rectangle sourceRect);

    ImageFinderResult findMatchingImage(File sourceImage, File templateImage, double threshold);

    ImageFinderResult findMatchingImage(AppiumDriver driver, File templateImage, double threshold, Rectangle sourceRect);
}
