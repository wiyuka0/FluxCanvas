package com.wiyuka.fluxCanvas.persistence;

import com.wiyuka.fluxCanvas.FluxCanvas;

import java.io.*;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ResourceExtractor {

    private final FluxCanvas plugin;

    public ResourceExtractor(FluxCanvas plugin) {
        this.plugin = plugin;
    }

    public void extractDataToRoot(boolean replace) {
        File dataFolder = new File(".");

        File pluginFile = plugin.jarFile();

        try (JarFile jar = new JarFile(pluginFile)) {
            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String path = entry.getName();

                if (path.startsWith("data/")) {

                    String relativePath = path.substring("data/".length());

                    if (relativePath.isEmpty()) {
                        continue;
                    }

                    File outFile = new File(dataFolder, relativePath);

                    if (entry.isDirectory()) {
                        if (!outFile.exists()) {
                            outFile.mkdirs();
                        }
                        continue;
                    }

                    if (outFile.exists() && !replace) {
                        plugin.getLogger().info("文件已存在，跳过: " + outFile.getPath());
                        continue;
                    }

                    if (outFile.getParentFile() != null) {
                        outFile.getParentFile().mkdirs();
                    }

                    try (InputStream in = jar.getInputStream(entry);
                         FileOutputStream out = new FileOutputStream(outFile)) {

                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = in.read(buffer)) > 0) {
                            out.write(buffer, 0, length);
                        }
                        plugin.getLogger().info("已解压文件到根目录: " + outFile.getPath());
                    } catch (IOException e) {
                        plugin.getLogger().severe("无法写入文件: " + outFile.getPath());
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe("无法读取插件Jar文件！");
            e.printStackTrace();
        }

        var textureZip = new File(dataFolder, "texture.zip");

        var textureFolder = new File(dataFolder, "texture");

        if(textureFolder.exists()) return;

        try {
            unzip(textureZip.getAbsolutePath(), textureFolder.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void unzip(String zipFilePath, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {

            ZipEntry entry = zipIn.getNextEntry();

            while (entry != null) {
                String filePath = destDirectory + File.separator + entry.getName();
                File newFile = new File(filePath);

                String destDirCanonicalPath = destDir.getCanonicalPath();
                String newFileCanonicalPath = newFile.getCanonicalPath();

                if (!newFileCanonicalPath.startsWith(destDirCanonicalPath + File.separator)) {
                    throw new IOException("检测到非法 Zip 路径 (Zip Slip): " + entry.getName());
                }

                if (entry.isDirectory()) {
                    if (!newFile.exists()) {
                        newFile.mkdirs();
                    }
                } else {
                    File parent = newFile.getParentFile();
                    if (!parent.exists()) {
                        parent.mkdirs();
                    }

                    extractFile(zipIn, newFile);
                }

                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
    }

    private static void extractFile(ZipInputStream zipIn, File file) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
            byte[] bytesIn = new byte[4096];
            int read;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }
    }
}