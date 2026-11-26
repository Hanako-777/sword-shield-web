package com.swordshield.file.service;

import com.swordshield.file.domain.AssetFile;
import com.swordshield.file.repository.AssetFileRepository;
import io.minio.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;

/**
 * 文件存储：默认本地磁盘（离线演示零依赖），配置 MINIO_ENABLED=true 后切换 MinIO。
 * 两者共用同一套元数据表与相同的对外接口，前端无感知。
 */
@Service
public class FileService {
    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private final MinioClient minio;
    private final AssetFileRepository repo;
    private final String bucket;
    private final boolean minioEnabled;
    private final Path localDir;

    public FileService(MinioClient minio,
                       AssetFileRepository repo,
                       @Value("${minio.bucket:assets}") String bucket,
                       @Value("${minio.enabled:false}") boolean minioEnabled,
                       @Value("${minio.local-dir:./uploads}") String localDir) {
        this.minio = minio;
        this.repo = repo;
        this.bucket = bucket;
        this.minioEnabled = minioEnabled;
        this.localDir = Paths.get(localDir).toAbsolutePath().normalize();
        log.info("文件服务存储模式: {}，本地目录: {}", minioEnabled ? "MinIO" : "本地磁盘", this.localDir);
    }

    public boolean isMinioEnabled() {
        return minioEnabled;
    }

    public Path localPath(String objectName) {
        return resolveSafe(objectName);
    }

    public AssetFile putObject(String objectName, String originalName, String contentType,
                               long size, InputStream stream, Long ownerId) throws Exception {
        Path target = null;
        if (minioEnabled) {
            ensureBucket();
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .contentType(contentType)
                    .stream(stream, size, -1)
                    .build();
            minio.putObject(args);
        } else {
            Files.createDirectories(localDir);
            target = resolveSafe(objectName);
            Files.createDirectories(target.getParent());
            Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
        }

        AssetFile f = new AssetFile();
        f.setBucket(minioEnabled ? bucket : "local");
        f.setObjectName(objectName);
        f.setOriginalName(originalName);
        f.setContentType(contentType);
        f.setSize(size);
        f.setOwnerId(ownerId);
        f.setCreatedAt(Instant.now());
        AssetFile saved = repo.save(f);
        log.info("文件上传成功 objectName={} size={} owner={} mode={}",
                objectName, size, ownerId, minioEnabled ? "minio" : "local");
        return saved;
    }

    public String presignedGet(String objectName, int expireSeconds) throws Exception {
        if (!minioEnabled) {
            // 本地模式返回网关可访问的静态下载地址
            return "/api/file/raw?objectName=" + URLEncoder.encode(objectName, StandardCharsets.UTF_8);
        }
        ensureBucket();
        GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .method(Method.GET)
                .expiry(expireSeconds)
                .build();
        return minio.getPresignedObjectUrl(args);
    }

    public InputStream getLocal(String objectName) throws Exception {
        return Files.newInputStream(resolveSafe(objectName));
    }

    private Path resolveSafe(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            throw new IllegalArgumentException("文件不存在");
        }
        Path p = localDir.resolve(objectName).normalize();
        if (!p.startsWith(localDir)) {
            throw new IllegalArgumentException("非法的文件路径");
        }
        return p;
    }

    private void ensureBucket() throws Exception {
        boolean exists = minio.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minio.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }
}
