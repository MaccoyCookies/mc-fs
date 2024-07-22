package io.github.maccoycookies.mcfs;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * file download and upload controller
 */
@RestController
public class FileController {

    @Value("${mcfs.path}")
    private String uploadPath;

    @Value("${mcfs.backupUrl}")
    private String backupUrl;

    @Autowired
    private HttpSyncer httpSyncer;

    @SneakyThrows
    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        String filename = request.getHeader(httpSyncer.XFILENAME);
        boolean needSync = false;
        if (filename == null || filename.isBlank()) {
            filename = file.getOriginalFilename();
            needSync = true;
        }
        File dest = new File(uploadPath + "/" + filename);
        if (!dest.exists()) {
            dest.mkdirs();
        }
        file.transferTo(dest);
        if (needSync) {
            // 同步文件到backup
            httpSyncer.sync(dest, backupUrl);
        }
        return file.getOriginalFilename();
    }

    @SneakyThrows
    @RequestMapping("/download")
    public void download(String name, HttpServletResponse response) {
        String path = uploadPath + "/" + name;
        File file = new File(path);
        try (FileInputStream fileInputStream = new FileInputStream(file);
             InputStream inputStream = new BufferedInputStream(fileInputStream);
             ServletOutputStream outputStream = response.getOutputStream()) {
            // 添加一些response
            response.setCharacterEncoding("utf-8");
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;filename=" + name);
            response.setHeader("Content-Length", String.valueOf(file.length()));

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
