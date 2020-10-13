package com.clbee;

import org.apache.commons.io.FileUtils;
import sun.misc.BASE64Encoder;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.*;
import javax.imageio.stream.*;

public class Converter {
    static final Pattern imagePattern = Pattern.compile("^<image id=\"(\\w+)\" width=\"\\d+\" height=\"\\d+\" xlink:href=\"(data:image\\/(gif|png|jpeg|jpg);base64,((?s).*))\"/>");
    public static void getImage(Path pathname) {
        try {
            //파일 객체 생성
            File orgFile = new File(pathname.toString());
            FileReader filereader = new FileReader(orgFile);
            BufferedReader reader = new BufferedReader(filereader);

            File updatedFile = new File(pathname.getParent() + "/m_" + pathname.getFileName());
            BufferedWriter updatedWriter = new BufferedWriter(new FileWriter(updatedFile));

            String imageFolder = pathname.getParent() + "/" + "image";
            makeFolder(imageFolder);

            String line = "";
            Matcher matcher;

            while((line = reader.readLine()) != null) {
                matcher = imagePattern.matcher(line);
                if (matcher.find()) {
                    String[] bitmap = matcher.group(2).split(",");
                    String imageId = matcher.group(1);
                    writeToQualityImage(bitmap[1], imageFolder + "/" + imageId + ".jpg");
                    updatedWriter.write(String.format("<image id=\"%s\" xlink:href=\"image/%s.jpg\" />\n", imageId, imageId));
                }
                else {
                    updatedWriter.write(line + "\n");
                }
            }
            reader.close();
            updatedWriter.close();
            if (orgFile.exists())
                orgFile.delete();

            updatedFile.renameTo(new File(pathname.toString()));

        } catch (FileNotFoundException e) {
            // TODO: handle exception
        } catch(IOException e){
            System.out.println(e);
        }
        System.out.print(".");
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

    public static void writeToQualityImage(String base64, String filename) throws IOException {
        Iterator imageFormatIterator = ImageIO.getImageWritersByFormatName("jpeg");
        ImageWriter writer = (ImageWriter) imageFormatIterator.next();
        ImageWriteParam param = writer.getDefaultWriteParam();

        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality((float)0.25);   // an integer between 0 and 1

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