package org.pcMonitor.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    public static void makeZipFromFolder(File targetFolder, File outputZipFile,boolean verbose) throws IOException {
        if (!targetFolder.isDirectory()) {
            throw new IllegalArgumentException("The provided targetFolder must be a directory.");
        }
        if(verbose) System.out.println("Creating zip file from target: " + targetFolder.getAbsolutePath());

        try (FileOutputStream fileOutputStream = new FileOutputStream(outputZipFile);
             ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {

            Path folderPath = targetFolder.toPath();
            Files.walk(folderPath).forEach(path -> {
                File file = path.toFile();
                try {
                    String zipEntryName = folderPath.relativize(path).toString().replace("\\", "/");

                    if (file.isDirectory()) {
                        // Add directory entry
                        if (!zipEntryName.endsWith("/")) {
                            zipEntryName += "/";
                        }
                        zipOutputStream.putNextEntry(new ZipEntry(zipEntryName));
                        zipOutputStream.closeEntry();
                        if(verbose) System.out.println("\tAdded directory: " + zipEntryName);
                    } else {
                        // Add file entry
                        zipOutputStream.putNextEntry(new ZipEntry(zipEntryName));
                        try (FileInputStream fileInputStream = new FileInputStream(file)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                                zipOutputStream.write(buffer, 0, bytesRead);
                            }
                        }
                        zipOutputStream.closeEntry();
                        if(verbose) System.out.println("\tAdded file: " + zipEntryName);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
        if(verbose) System.out.println("Created zip file: " + outputZipFile.getAbsolutePath());
    }

}