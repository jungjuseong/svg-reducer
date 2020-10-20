package com.clbee;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import sun.misc.BASE64Encoder;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.*;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.*;

import static javax.print.attribute.ResolutionSyntax.DPI;

public class Converter {
    static final Pattern imagePattern = Pattern.compile("^<image id=\"(\\w+)\" width=\"(\\d+)\" height=\"(\\d+)\" xlink:href=\"(data:image/(gif|png|jpeg|jpg);base64,((?s).*))\"/>");
    static final Pattern pathPattern = Pattern.compile("^<path style=\"((?s).*)\" d=\"((?s).*)\"/>");
    static final Pattern pathWithTransformPattern = Pattern.compile("^<path style=\"((?s).*)\" d=\"((?s).*)\" (transform=\"((?s).*)\")/>");

    static final Pattern numberPattern = Pattern.compile("^([-+]?[0-9]*\\.?[0-9]+)$");

    public static void smallerPath(String[] paths, BufferedWriter workWriter) throws IOException {
        Iterator pathIterator = Arrays.stream(paths).iterator();
        boolean spaced = true;
        while (pathIterator.hasNext()) {
            String nextString = pathIterator.next().toString();
            Matcher numberMatcher = numberPattern.matcher(nextString);
            if (numberMatcher.find()) {
                float coordinate = Float.parseFloat(numberMatcher.group(1));

                if (coordinate < 0 || spaced)
                    workWriter.write(String.format("%.2f", coordinate));
                else
                    workWriter.write(String.format(" %.2f", coordinate));

                spaced = false;
            }
            else {
                workWriter.write(nextString);
                spaced = true;
            }
        }
    }

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
                    writeToScaledImage(bitmap[1], imageFolder + "/" + imageId + ".jpg", imageScale);

                    workWriter.write(String.format("<image id=\"%s\" width=\"%s\" height=\"%s\" xlink:href=\"image/%s.jpg\" />\n", imageId, width, height, imageId));
                    System.out.print(".");
                    continue;
                }
                Matcher pathWithTransformMatcher = pathWithTransformPattern.matcher(line);
                if (pathWithTransformMatcher.find()) {
                    String[] paths = pathWithTransformMatcher.group(2).split("\\s");
                    workWriter.write("<path style=\"" + pathWithTransformMatcher.group(1) + "\" d=\"");
                    Converter.smallerPath(paths, workWriter);
                    workWriter.write("\" " + pathWithTransformMatcher.group(3) + "/>\n");
                    continue;
                }
                Matcher pathMatcher = pathPattern.matcher(line);
                if (pathMatcher.find()) {
                    String[] paths = pathMatcher.group(2).split("\\s");
                    workWriter.write("<path style=\"" + pathMatcher.group(1) + "\" d=\"");
                    Converter.smallerPath(paths, workWriter);
                    workWriter.write("\"/>\n");
                    continue;
                }
                workWriter.write(line.trim() + "\n");
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

    public static void renameFile(String source, String target) {
        try {
            File file = FileUtils.getFile(source);
            File fileToMove = FileUtils.getFile(target);
            FileUtils.moveDirectory(file, fileToMove);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static int pageSizeInKB(PDPage page) {
        PDDocument tempFile = null;
        ByteArrayOutputStream baos = null;

        try {
            baos = new ByteArrayOutputStream();
            tempFile = new PDDocument();
            tempFile.addPage(page);
            tempFile.save(baos);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                tempFile.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            try {
                baos.flush();
                baos.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return baos.size() / 1024;
    }


    private static void saveBufferedImage(BufferedImage bufferedImage, File output, int DPI, float quality) throws IOException {
        output.delete();

        final String formatName = "jpg";

        for (Iterator<ImageWriter> imageWriter = ImageIO.getImageWritersByFormatName(formatName); imageWriter.hasNext();) {
            ImageWriter nextWriter = imageWriter.next();
            ImageWriteParam writeParam = nextWriter.getDefaultWriteParam();
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            writeParam.setCompressionQuality(quality);

            ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);
            IIOMetadata metadata = nextWriter.getDefaultImageMetadata(typeSpecifier, writeParam);
            if (metadata.isReadOnly() || !metadata.isStandardMetadataFormatSupported()) {
                continue;
            }

            setDPI(metadata, DPI);

            final ImageOutputStream stream = ImageIO.createImageOutputStream(output);
            try {
                nextWriter.setOutput(stream);
                nextWriter.write(metadata, new IIOImage(bufferedImage, null, metadata), writeParam);
            } finally {
                stream.close();
            }
            break;
        }
    }

    private static void setDPI(IIOMetadata metadata, final int DPI) throws IIOInvalidTreeException {

        // for PMG, it's dots per millimeter
        double dotsPerMilli = 1.0 * DPI / 0.254f;

        IIOMetadataNode horizontalNode = new IIOMetadataNode("HorizontalPixelSize");
        horizontalNode.setAttribute("value", Double.toString(dotsPerMilli));

        IIOMetadataNode verticalNode = new IIOMetadataNode("VerticalPixelSize");
        verticalNode.setAttribute("value", Double.toString(dotsPerMilli));

        IIOMetadataNode dim = new IIOMetadataNode("Dimension");
        dim.appendChild(horizontalNode);
        dim.appendChild(verticalNode);

        IIOMetadataNode rootNode = new IIOMetadataNode("javax_imageio_1.0");
        rootNode.appendChild(dim);

        metadata.mergeTree("javax_imageio_1.0", rootNode);
    }

    public static int pdf2Imaga(String docFile, String prefix, String imageType, int dpi, float quality, int bigSize) throws IOException {
        InputStream docInputStream = new FileInputStream(docFile);

        PDDocument document = PDDocument.load(docInputStream);
        PDFRenderer pdfRenderer = new PDFRenderer(document);

        String fileNameWithoutExt = FilenameUtils.getBaseName(docFile);

        String targetPath = "./" + fileNameWithoutExt + "_images_" + dpi + "dpi_" + quality;
        if (Paths.get(docFile).getParent() != null) {
            targetPath = Paths.get(docFile).getParent().toString() + "/" + fileNameWithoutExt + "_images_" + dpi + "dpi_" + quality;
        }
        Files.createDirectories(Paths.get(targetPath));

        for (int pageNo = 0; pageNo < document.getPages().getCount(); pageNo++) {

            float heightInPt = document.getPage(pageNo).getMediaBox().getHeight();
            float widthInPt = document.getPage(pageNo).getMediaBox().getWidth();

            long pageSize = pageSizeInKB(document.getPage(pageNo));
            System.out.println(String.format("%d: (%.2fpt,%.2fpt) - %dK",pageNo,widthInPt, heightInPt, pageSize));

            String targetFilename = targetPath + "/" + prefix + pageNo + "." + imageType;

            BufferedImage image = pdfRenderer.renderImageWithDPI(pageNo, dpi, ImageType.RGB);

            int real_dpi = dpi;
            if (pageSize > bigSize) {
                real_dpi += dpi >> 2;
            }

            saveBufferedImage(image, new File(targetFilename), real_dpi, quality);
        }
        System.out.println("\nDone");
        document.close();
        docInputStream.close();

        return document.getPages().getCount();
    }
}