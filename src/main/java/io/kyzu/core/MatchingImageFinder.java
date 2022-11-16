package io.kyzu.core;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.openqa.selenium.OutputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.kyzu.exceptions.ImageNotFoundException;
import io.kyzu.interfaces.IMatchingImageFinder;
import io.kyzu.models.MatchingMethod;
import io.kyzu.models.constants.PnPContants;
import io.kyzu.utils.Converter;

import io.appium.java_client.AppiumDriver;
import nu.pattern.OpenCV;

public class MatchingImageFinder implements IMatchingImageFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static {
        OpenCV.loadShared();
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
    }

    MatchingMethod matchingMethod;

    public MatchingImageFinder() {
        this.matchingMethod = MatchingMethod.MM_SQDIFF_NORMED;
    }

    public MatchingImageFinder(MatchingMethod matchingMethod) {
        this.matchingMethod = matchingMethod;
    }

    /**
     * Finds a template image in a source image. Throws an exception when the
     * image wasn't found or the desired accuracy couldn't be met.
     *
     * @param sourceMat The source image.
     * @param templateMat The template image to find in the source image.
     * @param threshold The desired accuracy of the find operation as a
     * number between 0 and 1.
     * @return An ImageFinderResult object that stores the rectangle of the
     * found image and desired accuracy.
     */
    private ImageFinderResult findMatchingImage(Mat sourceMat, Mat templateMat, double threshold) {
        if (sourceMat.width() < templateMat.width() || sourceMat.height() < templateMat.height()) {
            throw new UnsupportedOperationException("The template image is larger than the source image. Ensure that the width and/or height of the image you are trying to find do not exceed the dimensions of the source image.");
        }

        Mat result = new Mat(sourceMat.rows() - templateMat.rows() + 1, sourceMat.rows() - templateMat.rows() + 1, CvType.CV_32FC1);
        int intMatchingMethod = this.matchingMethod.getTemplateMat();

        Imgproc.matchTemplate(sourceMat, templateMat, result, intMatchingMethod);
        MinMaxLocResult minMaxLocRes = Core.minMaxLoc(result);

        double accuracy = 0;
        Point location = null;

        if (this.matchingMethod.getTemplateMat() == Imgproc.TM_SQDIFF_NORMED) {
            accuracy = 1 - minMaxLocRes.minVal;
            location = minMaxLocRes.minLoc;
        } else {
            accuracy = minMaxLocRes.maxVal;
            location = minMaxLocRes.maxLoc;
        }

        if (accuracy < threshold) {
            throw new ImageNotFoundException(
                    String.format(
                            "Failed to find template image in the source image. The accuracy was %.2f and the desired accuracy was %.2f",
                            accuracy,
                            threshold),
                    new Rectangle((int) location.x, (int) location.y, templateMat.width(), templateMat.height()),
                    accuracy);
        }

        if (!minMaxLocResultIsValid(minMaxLocRes)) {
            throw new ImageNotFoundException(
                    "Image find result (MinMaxLocResult) was invalid. This usually happens when the source image is covered in one solid color.",
                    null,
                    null);
        }

        Rectangle foundRect = new Rectangle(
                (int) location.x,
                (int) location.y,
                templateMat.width(),
                templateMat.height());

        return new ImageFinderResult(foundRect, accuracy);
    }

    /**
     * Finds a template image on the screen. Throws an exception when the image
     * wasn't found or the desired accuracy couldn't be met.
     *
     * @param templateImage The template image to find.
     * @param threshold The desired accuracy of the find operation as a
     * number between 0 and 1.
     * @param sourceRect The rectangle on the screen to look into.
     * @return An ImageFinderResult object that stores the rectangle of the
     * found image and desired accuracy.
     */
    @Override
    public ImageFinderResult findMatchingImage(AppiumDriver driver, BufferedImage templateImage, double threshold,
                                               Rectangle sourceRect) {
        try {
            BufferedImage capture = ImageIO.read(driver.getScreenshotAs(OutputType.FILE));
            if (sourceRect != null) {
                capture = capture.getSubimage(
                        sourceRect.x,
                        sourceRect.y,
                        sourceRect.width,
                        sourceRect.height);
            }

            Mat sourceMat = Converter.convertToMat(capture);
            Mat templateMat = Converter.convertToMat(templateImage);
            return findMatchingImage(sourceMat, templateMat, threshold);
        } catch (Exception ex) {
            throw new ImageNotFoundException(PnPContants.ERR_FIND_IMG_STR, sourceRect, threshold);
        }
    }

    /**
     * Finds any one of the template images in a source image. The method
     * iterates through the template images, performs a find operation for each
     * one of them and selects the one that produced the best accuracy. Throws
     * an exception when no image was found or the desired accuracy couldn't be
     * met.
     *
     * @param sourceImage The source image.
     * @param templateImages The collection of template images to look for in
     * the source image.
     * @param threshold The desired accuracy of the find operation as a
     * number between 0 and 1.
     * @param sourceRect The rectangle in the source image to look into. If
     * null, the find operation will look into the whole source image.
     * @return An ImageFinderResult object that stores the rectangle of the
     * found image and desired accuracy.
     */
    @Override
    public ImageFinderResult findAnyMatchingImage(BufferedImage sourceImage, List<BufferedImage> templateImages,
                                                  double threshold, Rectangle sourceRect) {
        ImageFinderResult bestResult = new ImageFinderResult(new Rectangle(100, 100, 100, 100), 0);

        for (BufferedImage templateImage : templateImages) {
            try {
                ImageFinderResult result = findMatchingImage(sourceImage, templateImage, 0, sourceRect);
                LOGGER.info("Image was found at the threshold of: " + result.getThreshold());
                if (result.getThreshold() > bestResult.getThreshold() && result.getThreshold() >= threshold) {
                    bestResult = result;
                    break;
                }
            } catch (UnsupportedOperationException ex) {
                LOGGER.error("The template image is larger than the source image", ex);
            } catch (Exception ex) {
                LOGGER.warn("Failed to perform an image template matching operation", ex);
            }
        }

        if (bestResult.getThreshold() < threshold) {
            String messagePrefix = templateImages.size() == 1
                                   ? "Failed to find the template image"
                                   : String.format("Failed to find one of %s template images", templateImages.size());

            throw new ImageNotFoundException(
                    imageNotFoundExString(messagePrefix, bestResult, threshold, sourceRect), bestResult.getImageLocation(), bestResult.getThreshold());
        }

        return bestResult;
    }

    /**
     * Finds any one of the template images in a source image. The method
     * iterates through the template images, performs a find operation for each
     * one of them and selects the one that produced the best accuracy. Throws
     * an exception when no image was found or the desired accuracy couldn't be
     * met.
     *
     * @param sourceImage The source image.
     * @param templateImages The collection of template images to look for in
     * the source image.
     * @param threshold The desired accuracy of the find operation as a
     * number between 0 and 1.
     * @param sourceRect The rectangle in the source image to look into. If
     * null, the find operation will look into the whole source image.
     * @return An ImageFinderResult object that stores the rectangle of the
     * found image and desired accuracy.
     */
    @Override
    public ImageFinderResult findAnyMatchingImage(File sourceImage, List<File> templateImages, double threshold,
                                                  Rectangle sourceRect) {
        ImageFinderResult bestResult = new ImageFinderResult(new Rectangle(100, 100, 100, 100), 0);
        try {
            BufferedImage srcBuff = ImageIO.read(sourceImage);
            for (File templateFile : templateImages) {
                try {
                    BufferedImage file = ImageIO.read(templateFile);
                    ImageFinderResult result = findMatchingImage(srcBuff, file, 0, sourceRect);
                    LOGGER.info("Image was found at the threshold of: " + result.getThreshold());
                    if (result.getThreshold() > bestResult.getThreshold() && result.getThreshold() >= threshold) {
                        bestResult = result;
                        break;
                    }
                } catch (UnsupportedOperationException ex) {
                    LOGGER.error("The template image is larger than the source image", ex);
                } catch (Exception ex) {
                    LOGGER.warn("Failed to perform an image template matching operation", ex);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        if (bestResult.getThreshold() < threshold) {
            String messagePrefix = templateImages.size() == 1
                                   ? "Failed to find the template image"
                                   : String.format("Failed to find one of %s template images", templateImages.size());

            throw new ImageNotFoundException(
                   imageNotFoundExString(messagePrefix, bestResult, threshold, sourceRect), bestResult.getImageLocation(), bestResult.getThreshold());
        }

        return bestResult;
    }

    /**
     * Finds a template image in a source image. Throws an exception when the
     * image wasn't found or the desired accuracy couldn't be met.
     *
     * @param sourceImage The source image.
     * @param templateImage The template image to find in the source image.
     * @param threshold The desired accuracy of the find operation as a
     * number between 0 and 1.
     * @param sourceRect The rectangle in the source image to look into. If
     * null, the find operation will look into the whole source image.
     * @return An ImageFinderResult object that stores the rectangle of the
     * found image and desired accuracy.
     */
    @Override
    public ImageFinderResult findMatchingImage(BufferedImage sourceImage, BufferedImage templateImage, double threshold,
                                               Rectangle sourceRect) {
        BufferedImage subImage = sourceImage;
        if (sourceRect != null) {
            subImage = sourceImage.getSubimage(
                    sourceRect.x,
                    sourceRect.y,
                    sourceRect.width,
                    sourceRect.height);
        }

        Mat sourceMat = Converter.convertToMat(subImage);
        Mat templateMat = Converter.convertToMat(templateImage);

        return findMatchingImage(sourceMat, templateMat, threshold);
    }

    /**
     * Finds a template image in a source image. Throws an exception when the
     * image wasn't found or the desired accuracy couldn't be met.
     *
     * @param sourceImage The source image.
     * @param templateImage The template image to find in the source image.
     * @param threshold The desired accuracy of the find operation as a
     * number between 0 and 1.
     * @return An ImageFinderResult object that stores the rectangle of the
     * found image and desired accuracy.
     */
    @Override
    public ImageFinderResult findMatchingImage(File sourceImage, File templateImage, double threshold) {
        Mat sourceMat = Imgcodecs.imread(sourceImage.getAbsolutePath());
        Mat templateMat = Imgcodecs.imread(templateImage.getAbsolutePath());
        return findMatchingImage(sourceMat, templateMat, threshold);
    }

    /**
     * Finds a template image on the screen. Throws an exception when the image
     * wasn't found or the desired accuracy couldn't be met.
     *
     * @param templateImage The template image to find.
     * @param threshold The desired accuracy of the find operation as a
     * number between 0 and 1.
     * @param sourceRect The rectangle on the screen to look into.
     * @return An ImageFinderResult object that stores the rectangle of the
     * found image and desired accuracy.
     */
    @Override
    public ImageFinderResult findMatchingImage(AppiumDriver driver, File templateImage, double threshold, Rectangle sourceRect) {
        try {
            BufferedImage capture = ImageIO.read(driver.getScreenshotAs(OutputType.FILE));
            if (sourceRect != null) {
                capture = capture.getSubimage(
                        sourceRect.x,
                        sourceRect.y,
                        sourceRect.width,
                        sourceRect.height);
            }

            Mat sourceMat = Converter.convertToMat(capture);
            Mat templateMat = Imgcodecs.imread(templateImage.getAbsolutePath());
            return findMatchingImage(sourceMat, templateMat, threshold);
        } catch (Exception ex) {
            throw new ImageNotFoundException(PnPContants.ERR_FIND_IMG_STR, sourceRect, threshold);
        }
    }

    /**
     * Checks whether an OpenCV MinMaxLocResult object is valid. This object is
     * used for storing the location of the minimum and maximum values for an
     * image find operation, along with the actual values themselves.
     */
    private boolean minMaxLocResultIsValid(MinMaxLocResult minMaxLocRes) {
        if (minMaxLocRes.minVal == 1
            && minMaxLocRes.maxVal == 1
            && minMaxLocRes.maxLoc.x == 0
            && minMaxLocRes.maxLoc.y == 0
            && minMaxLocRes.minLoc.x == 0
            && minMaxLocRes.minLoc.y == 0) {

            return false;
        } else {
            return true;
        }
    }

    /**
     * ImageNotFoundException String
     */
    private String imageNotFoundExString(String messagePrefix, ImageFinderResult bestResult, double threshold, Rectangle sourceRect) {
        if (sourceRect != null) {
            return String.format(
                            "%s in the source image at (%s, %s, %s, %s). The best accuracy was %.2f and the desired accuracy was %.2f",
                            messagePrefix,
                            sourceRect.x,
                            sourceRect.y,
                            sourceRect.width,
                            sourceRect.height,
                            bestResult.getThreshold(),
                            threshold);
        } else {
            return String.format(
                            "%s in the source image. The best accuracy was %.2f and the desired accuracy was %.2f",
                            messagePrefix,
                            bestResult.getThreshold(),
                            threshold);
        }
    }
}
