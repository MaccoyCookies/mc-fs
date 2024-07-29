package io.github.maccoycookies.mcfs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Maccoy
 * @date 2024/7/27 22:44
 * Description file meta data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileMeta {

    private String name;

    private String originalFilename;

    private long size;

    private Map<String, String> tags = new HashMap<>();

    private String downloadUrl;

}
