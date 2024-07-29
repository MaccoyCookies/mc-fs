package io.github.maccoycookies.mcfs;

import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 *
 */
@Component
public class MqSyncer {

    @Value("${mcfs.path}")
    private String uploadPath;

    @Value("${mcfs.downloadUrl}")
    private String localUrl;


    @Autowired
    RocketMQTemplate rocketMQTemplate;

    private String topic = "mcfs";

    public void sync(FileMeta fileMeta) {
        Message<String> message = MessageBuilder.withPayload(JSON.toJSONString(fileMeta)).build();
        rocketMQTemplate.send(topic, message);
        System.out.println("===> send message: " + message);
    }

    @Service
    @RocketMQMessageListener(topic = "mcfs", consumerGroup = "${mcfs.group}")
    public class FileMqSyncer implements RocketMQListener<MessageExt> {
        @Override
        public void onMessage(MessageExt messageExt) {
            // 1. 从消息里拿到meta数据
            System.out.println("===> onMessage ID = " + messageExt.getMsgId());
            String json = new String(messageExt.getBody());
            FileMeta fileMeta = JSON.parseObject(json, FileMeta.class);
            if (fileMeta.getDownloadUrl() == null || fileMeta.getDownloadUrl().isBlank()) {
                System.out.println("===> downloadUrl is blank.");
                return;
            }

            // 去重本机操作
            if (fileMeta.getDownloadUrl().equals(localUrl)) {
                System.out.println("===> the same file server, ignore sync task.");
                return;
            }
            System.out.println("===> the other file server, process sync task.");

            // 2. 写meta文件
            String dir = uploadPath + "/" + fileMeta.getName().substring(0,2);
            File metaFile = new File(dir, fileMeta.getName() + ".meta");
            if (metaFile.exists()) {
                System.out.println("===> meta file exists and ignore save: " + metaFile.getAbsolutePath());
            } else {
                System.out.println("===> meta file save: " + metaFile.getAbsolutePath());
                FileUtil.writeString(metaFile, json);
            }

            // 3. 下载文件
            File file = new File(dir, fileMeta.getName());
            if (file.exists() && file.length() == fileMeta.getSize()) {
                System.out.println("===> file exists and ignore download: " + file.getAbsolutePath());
                return;
            }
            String downloadUrl = fileMeta.getDownloadUrl() + "?name=" + file.getName();
            FileUtil.download(downloadUrl, file);
        }
    }
}
