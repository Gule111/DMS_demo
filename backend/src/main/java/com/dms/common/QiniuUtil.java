package com.dms.common;

import com.google.gson.Gson;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.UUID;

/**
 * 七牛云 OSS 文件上传工具类
 */
@Component
public class QiniuUtil {

    @Value("${dms.qiniu.access-key}")
    private String accessKey;

    @Value("${dms.qiniu.secret-key}")
    private String secretKey;

    @Value("${dms.qiniu.bucket}")
    private String bucket;

    @Value("${dms.qiniu.domain}")
    private String domain;

    /**
     * 上传文件流到七牛云
     *
     * @param inputStream 文件输入流
     * @param directory   目标文件夹路径（如 "uploads/id_cards/front/"），必须以 "/" 结尾或为空
     * @param originalFilename 原始文件名
     * @return 返回完整的外链访问 URL
     */
    public String uploadFile(InputStream inputStream, String directory, String originalFilename) {
        // 构造一个带指定 Region 对象的配置类 (Region.autoRegion() 会自动匹配你机房所在的区域)
        Configuration cfg = new Configuration(Region.autoRegion());
        // 其他参数参考类注释
        UploadManager uploadManager = new UploadManager(cfg);

        // 生成唯一的文件名，防止覆盖
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String key = (directory != null ? directory : "") + uuid + suffix;

        try {
            Auth auth = Auth.create(accessKey, secretKey);
            String upToken = auth.uploadToken(bucket);

            // 核心上传动作
            Response response = uploadManager.put(inputStream, key, upToken, null, null);

            // 解析上传成功的结果
            DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
            
            // 返回完整的公网访问 URL（注意补全 http/https 前缀）
            return "http://" + domain + "/" + putRet.key;
            
        } catch (Exception ex) {
            throw new RuntimeException("七牛云 OSS 文件上传失败: " + ex.getMessage());
        }
    }
}
