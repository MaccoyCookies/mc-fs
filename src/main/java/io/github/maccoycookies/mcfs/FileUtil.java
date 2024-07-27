package io.github.maccoycookies.mcfs;

import java.io.File;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.UUID;

/**
 * @author Maccoy
 * @date 2024/7/27 18:48
 * Description
 */
public class FileUtil {

    static String DEFAULT_MIME_TYPE = "application/octet-stream";

    public static String getMimeTYpe(String fileName) {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String contentType = fileNameMap.getContentTypeFor(fileName);
        return contentType == null ? DEFAULT_MIME_TYPE : contentType;
    }

    public static void init(String uploadPath) {
        File dir = new File(uploadPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        for (int i = 0; i < 256; i++) {
            String subDir = String.format("%02x", i);
            File file = new File(uploadPath + "/" + subDir);
            if (!file.exists()) {
                file.mkdirs();
            }
        }
    }

    public static String getExt(String originalFilename) {
        return originalFilename.substring(originalFilename.indexOf("."));
    }

    public static String getUUIDFilename(String originalFilename) {
        return UUID.randomUUID().toString() + "/" + getExt(originalFilename);
    }

    public static String getSubDir(String filename) {
        return filename.substring(0, 2);
    }
}
