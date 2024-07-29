package io.github.maccoycookies.mcfs;

import com.alibaba.fastjson.JSON;
import jakarta.servlet.ServletOutputStream;
import lombok.SneakyThrows;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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

    @SneakyThrows
    public static void writeMeta(File metaFile, FileMeta fileMeta) {
        String json = JSON.toJSONString(fileMeta);
        Files.writeString(Paths.get(metaFile.toURI()), json, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }

    @SneakyThrows
    public static void writeString(File file, String content) {
        Files.writeString(Paths.get(file.getAbsolutePath()), content, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }

    public static void download(String downloadUrl, File file) {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<?> entity = new HttpEntity<>(new HttpHeaders());
        ResponseEntity<Resource> exchange = restTemplate.exchange(downloadUrl, HttpMethod.GET, entity, Resource.class);

        try (InputStream inputStream = new BufferedInputStream(exchange.getBody().getInputStream());
             OutputStream outputStream = new FileOutputStream(file)) {
            // 读取文件 并逐段输出
            byte[] buffer = new byte[16 * 1024];
            while (inputStream.read(buffer) != -1) {
                outputStream.write(buffer);
            }
            outputStream.flush();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
