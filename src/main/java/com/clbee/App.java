package com.clbee;

import javax.swing.text.html.Option;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class App
{
    private static List<Path> walk(Path path) throws IOException {
        List<Path> paths = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    walk(entry);
                }
                paths.add(entry);
            }
        }
        return paths;
    }

    public static Optional<String> getExtension(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

    public static void extractImageFromSVG(String root) throws IOException {
        float IMAGE_SCALE = (float) 0.9;
        Path myPath = Paths.get(root);
        List<Path> paths = walk(myPath);

        paths.forEach(path ->
        {
            if (Files.isRegularFile(path)) {
                Optional<String> xml = getExtension(path.toString());
                if (xml.isPresent() && "svg".equals(xml.get())) {
                    Converter.reduceSize(path, IMAGE_SCALE,2);
                }
            }
        });

    }

    private static class CommandLineOption {
        String flag;
        String value;
        public CommandLineOption(String flag, String value) {
            this.flag = flag;
            this.value = value;
        }
    }

    public static void main(String[] args) throws IOException {

        if (args.length == 0) {
            System.out.println("usage: java -jar svg-reducer abc.pdf -dpi 140 -q 0.9 -big 500");
            return;
        }

        List<String> argsList = new ArrayList<>();
        List<CommandLineOption> optionsList = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].charAt(0) == '-') {
                if (args[i].length() < 2)
                    throw new IllegalArgumentException("Not a valid argument: " + args[i]);

                if (args.length - 1 == i)
                    throw new IllegalArgumentException("Expected arg after: " + args[i]);

                optionsList.add(new CommandLineOption(args[i], args[i + 1]));
                i++;
            }
            else
                argsList.add(args[i]);
        }

        int dpi = 144;
        float quality = 0.9f;
        int bigSize = 500;

        for (CommandLineOption option : optionsList) {
            switch (option.flag) {
                case "-q":
                    quality = Float.parseFloat(option.value);
                    break;
                case "-dpi":
                    dpi = Integer.parseInt(option.value);
                    break;
                case "-big":
                    bigSize = Integer.parseInt(option.value);
                    break;
                default:
                    break;
            }
        }
        System.out.println(String.format("%s:%s dpi:%s quality:high quality if more than %sK",argsList.get(0),dpi,quality,bigSize));

        Converter.pdf2Imaga(argsList.get(0),"images","jpg", dpi, quality, bigSize);
    }
}
