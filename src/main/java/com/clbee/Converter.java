package com.clbee;

import org.apache.commons.io.FileUtils;
import sun.misc.BASE64Encoder;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.*;
import javax.imageio.stream.*;

public class Converter {
    static final Pattern imagePattern = Pattern.compile("^<image id=\"(\\w+)\" width=\"(\\d+)\" height=\"(\\d+)\" xlink:href=\"(data:image/(gif|png|jpeg|jpg);base64,((?s).*))\"/>");
    static final Pattern pathPattern = Pattern.compile("^<path style=\"((?s).*)\" d=\"((?s).*)\"/>");
    static final Pattern numberPattern = Pattern.compile("^([-+]?[0-9]*\\.?[0-9]+)$");

    public static void reduceSize(Path sourcePath, float imageScale, int precision) {
        try {
            File sourceFile = new File(sourcePath.toString());
            BufferedReader reader = new BufferedReader(new FileReader(sourceFile));

            String workPath = sourcePath.getParent() + "/" + "m_" + sourcePath.getFileName();
            File workFile = new File(workPath);
            BufferedWriter workWriter = new BufferedWriter(new FileWriter(workFile));

            String imageFolder = sourcePath.getParent() + "/" + "image";
            makeFolder(imageFolder);

            String line;
            Matcher matcher;

            while ((line = reader.readLine()) != null) {
                matcher = imagePattern.matcher(line);
                if (matcher.find()) {
                    String[] bitmap = matcher.group(4).split(",");
                    String imageId = matcher.group(1);
                    String width = matcher.group(2);
                    String height = matcher.group(3);
                    //writeToQualityImage(bitmap[1], imageFolder + "/" + imageId + ".jpg");
                    writeToScaledImage(bitmap[1], imageFolder + "/" + imageId + ".jpg", imageScale);

                    workWriter.write(String.format("<image id=\"%s\" width=\"%s\" height=\"%s\" xlink:href=\"image/%s.jpg\" />\n", imageId, width, height, imageId));
                    System.out.print(".");

                    continue;
                }
// <path style="stroke:none;" d="M 1.59375 1.59375 L 1.59375 -11.203125
// L 14.40625 -11.203125 L 14.40625 1.59375 Z M 2.40625 0.796875 L 13.59375 0.796875 L 13.59375 -10.40625 L 2.40625 -10.40625 Z M 2.40625 0.796875 "/>

                Matcher pathMatcher = pathPattern.matcher(line);
                if (pathMatcher.find()) {
                    String[] paths = pathMatcher.group(2).split("\\s");
                    Iterator it = Arrays.stream(paths).iterator();

                    workWriter.write("<path style=\"" + pathMatcher.group(1) + "\" d=\"");

                    boolean delimetered = true;
                    while (it.hasNext()) {
                        String nextString = it.next().toString();
                        Matcher numberMatcher = numberPattern.matcher(nextString);
                        if (numberMatcher.find()) {
                            float f = Float.parseFloat(numberMatcher.group(1));
                            String formatString;
                            if (precision == 3) {
                                formatString = String.format("%.3f", f);
                            }
                            else {
                                formatString = String.format("%.2f", f);
                            }
                            if (f < 0) {
                                workWriter.write(formatString);
                            }
                            else {
                                if (delimetered)
                                    workWriter.write(formatString);
                                else
                                    workWriter.write(" " + formatString);
                            }
                            delimetered = false;
                        }
                        else {
                            workWriter.write(nextString);
                            delimetered = true;
                        }
                    }
                    workWriter.write("\"/>");
                }
                else {
                    workWriter.write(line);
                }
            }
            reader.close();
            workWriter.close();

            if (sourceFile.delete())
                workFile.renameTo(new File(sourcePath.toString()));
        }
        catch (FileNotFoundException e) {
            // TODO: handle exception
        }
        catch(IOException e){
            System.out.println(e);
        }
    }

    private static BufferedImage createImageFromBas64String(String imageData) {
        byte[] decodedImage = Base64.getDecoder().decode(imageData.getBytes(StandardCharsets.UTF_8));
        ByteArrayInputStream input = new ByteArrayInputStream(decodedImage);
        try {
            return ImageIO.read(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String encodeToBase64(BufferedImage image, String type) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        ImageIO.write(image, type, os);
        byte[] imageBytes = os.toByteArray();
        BASE64Encoder base64Encoder= new BASE64Encoder();

        return base64Encoder.encode(imageBytes);
    }

    public static void writeToScaledImage(String base64, String filename, float scale) throws IOException {
        BufferedImage bImage = createImageFromBas64String(base64);
        ImageResizer.resize(bImage, filename, scale);
    }

    public static void writeToQualityImage(String base64, String filename) throws IOException {
        Iterator imageFormatIterator = ImageIO.getImageWritersByFormatName("jpeg");
        ImageWriter writer = (ImageWriter) imageFormatIterator.next();
        ImageWriteParam param = writer.getDefaultWriteParam();

        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality((float) 0.5);   // an integer between 0 and 1

        File imageFile = new File(filename);
        FileImageOutputStream output = new FileImageOutputStream(imageFile);
        writer.setOutput(output);

        BufferedImage bImage = createImageFromBas64String(base64);
        IIOImage image = new IIOImage(bImage, null, null);
        writer.write(null, image, param);
        writer.dispose();
    }

    public static void makeFolder(String pathname) {
        File Folder = new File(pathname);

        if (!Folder.exists()) {
            try {
                Folder.mkdir();
            }
            catch(Exception e){
                e.getStackTrace();
            }
        }
    }

    public void renameFile(String source, String target) {
        try {
            File file = FileUtils.getFile(source);
            File fileToMove = FileUtils.getFile(target);
            FileUtils.moveDirectory(file, fileToMove);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}