package io.kyzu.utils;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class Converter {
    public static BufferedImage convertToBufferedImage(Mat mat) {
        byte[] data = new byte[mat.cols() * mat.rows() * (int) mat.elemSize()];
        mat.get(0, 0, data);

        BufferedImage image = new BufferedImage(
                mat.cols(),
                mat.rows(),
                mat.channels() == 1
                ? BufferedImage.TYPE_BYTE_GRAY
                : BufferedImage.TYPE_3BYTE_BGR);
        image.getRaster().setDataElements(0, 0, mat.cols(), mat.rows(), data);
        return image;
    }

    /**
     * Converts a BufferedImage to an OpenCV Mat object.
     */
    public static Mat convertToMat(BufferedImage buffImg) {
        BufferedImage convertedImg = null;

        // Convert the image to TYPE_3BYTE_BGR, if necessary
        if (buffImg.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            convertedImg = buffImg;
        } else {
            convertedImg = new BufferedImage(buffImg.getWidth(), buffImg.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        }

        convertedImg.getGraphics().drawImage(buffImg, 0, 0, null);

        WritableRaster raster = convertedImg.getRaster();
        DataBufferByte data = (DataBufferByte) raster.getDataBuffer();
        byte[] pixels = data.getData();

        Mat mat = new Mat(buffImg.getHeight(), buffImg.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, pixels);
        return mat;
    }
}
