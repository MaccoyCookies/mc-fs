package io.github.maccoycookies.mcfs;

import com.alibaba.fastjson.JSON;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.DigestUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
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

    @Value("${mcfs.autoMd5}")
    private boolean autoMd5;

    @Autowired
    private HttpSyncer httpSyncer;

    @SneakyThrows
    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {

        // 1. 处理文件
        String filename = request.getHeader(HttpSyncer.XFILENAME);
        boolean needSync = false;
        String originalFilename = file.getOriginalFilename();
        // upload 上传文件
        if (filename == null || filename.isBlank()) {
            needSync = true;
            filename = FileUtil.getUUIDFilename(originalFilename);
        } else {
            // 同步文件
            String xor = request.getHeader(HttpSyncer.XORIGFILENAME);
            if (xor != null && !xor.isBlank()) {
                originalFilename = xor;
            }
        }

        String subDir = FileUtil.getSubDir(filename);
        File dest = new File(uploadPath + "/" + subDir + "/" + filename);
        file.transferTo(dest);
        // 2. 处理meta
        FileMeta fileMeta = new FileMeta();
        fileMeta.setName(filename);
        fileMeta.setOriginalFilename(originalFilename);
        fileMeta.setSize(file.getSize());
        if (autoMd5) {
            fileMeta.getTags().put("md5", DigestUtils.md5DigestAsHex(new FileInputStream(dest)));
        }

        // 2.1 存放到本地文件
        String metaFilename = filename + ".meta";
        File metaFile = new File(uploadPath + "/" + subDir + "/" + metaFilename);
        FileUtil.writeMeta(metaFile, fileMeta);
        // 2.2 存放到数据库
        // 2.3 存放到配置中心或者注册中心

        // 3. 同步文件到backup
        if (needSync) {
            httpSyncer.sync(dest, backupUrl, originalFilename);
        }
        return originalFilename;
    }

    @SneakyThrows
    @RequestMapping("/download")
    public void download(String name, HttpServletResponse response) {
        String subDir = FileUtil.getSubDir(name);
        String path = uploadPath + "/" + subDir + "/" + name;
        File file = new File(path);
        try (FileInputStream fileInputStream = new FileInputStream(file);
             InputStream inputStream = new BufferedInputStream(fileInputStream);
             ServletOutputStream outputStream = response.getOutputStream()) {
            // 添加一些response
            response.setCharacterEncoding("utf-8");
            response.setContentType(FileUtil.getMimeTYpe(name));
            // response.setHeader("Content-Disposition", "attachment;filename=" + name);
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

    @PostMapping("/meta")
    public String meta(String name) {
        String subDir = FileUtil.getSubDir(name);
        String path = uploadPath + "/" + subDir + "/" + name + ".meta";
        File file = new File(path);
        try {
            return FileCopyUtils.copyToString(new FileReader(file));
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

    }

}
