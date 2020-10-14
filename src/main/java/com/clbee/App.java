package com.clbee;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class App 
{
    private static List<Path> paths = new ArrayList<>();

    private static List<Path> walk(Path path) throws IOException {

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

    public static void main(String[] args) throws IOException {

        float IMAGE_SCALE = (float) 0.5;
        if (args.length > 0) {
            Path myPath = Paths.get(args[0]);
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
    }
}
