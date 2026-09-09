package com.foodcommunity.service;

import cn.hutool.core.util.StrUtil;
import com.foodcommunity.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

/**
 * 本地图片上传：保存到 upload.dir（默认 ./uploads，相对启动目录），
 * 通过 WebConfig 静态映射 /uploads/** 对外访问。
 * 后期如需换云 OSS，只需替换本类实现。
 */
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5MB

    private final Path uploadRoot;

    public FileStorageService(@Value("${upload.dir:./uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("图片大小不能超过 5MB");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = StrUtil.subAfter(original, ".", true).toLowerCase();
        if (StrUtil.isBlank(ext) || !ALLOWED_EXT.contains(ext)) {
            throw new BusinessException("仅支持 jpg/png/gif/webp 格式图片");
        }
        try {
            Files.createDirectories(uploadRoot);
            String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            file.transferTo(uploadRoot.resolve(filename).toFile());
            return "/uploads/" + filename;
        } catch (IOException e) {
            throw new BusinessException("图片保存失败，请重试");
        }
    }
}
