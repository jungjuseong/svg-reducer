package com.clbee;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ImageResizer {
    /**
     * Resizes an image to a absolute width and height (the image may not be proportional)
     *
     * @param inputImage  Original image
     * @param outputPath Path to save the resized image
     * @param scaledWidth     absolute width in pixels
     * @param scaledHeight    absolute height in pixels
     * @throws IOException 에러
     */
    public static void resize(BufferedImage inputImage, String outputPath, int scaledWidth, int scaledHeight)
            throws IOException {

        // creates output image
        BufferedImage outputImage = new BufferedImage(scaledWidth, scaledHeight, inputImage.getType());

        // scales the input image to the output image
        Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(inputImage, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();

        // extracts extension of output file
        String formatName = outputPath.substring(outputPath.lastIndexOf(".") + 1);

        // writes to output file
        ImageIO.write(outputImage, formatName, new File(outputPath));
    }

    /**
     * Resizes an image by a percentage of original size (proportional).
     *
     * @param inputImage  original image
     * @param outputPath Path to save the resized image
     * @param percent         a double number specifies percentage of the output image
     *                        over the input image.
     * @throws IOException 에러
     */
    public static void resize(BufferedImage inputImage, String outputPath, double percent) throws IOException {
        //File inputFile = new File(inputPath);
        //BufferedImage inputImage = ImageIO.read(inputFile);

        int scaledWidth = (int) (inputImage.getWidth() * percent);
        int scaledHeight = (int) (inputImage.getHeight() * percent);

        resize(inputImage, outputPath, scaledWidth, scaledHeight);
    }
}