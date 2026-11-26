package com.swordshield.file.service;

import com.swordshield.file.domain.AssetFile;
import com.swordshield.api.file.AssetFileDto;
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
 * 文件服务核心应用服务。
 *
 * <p>默认使用 MinIO；当 {@code minio.enabled=false} 时回退到本地磁盘，
 * 便于没有对象存储的离线演示环境。两种模式共用元数据表与对外接口。
 */
@Service
public class FileService {
    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private final MinioClient minio;
    private final AssetFileRepository repo;
    private final String bucket;
    private final boolean minioEnabled;
    private final Path localDir;

    public FileService(MinioClient minio, AssetFileRepository repo,
                       @Value("${minio.bucket}") String bucket,
                       @Value("${minio.enabled:true}") boolean minioEnabled,
                       @Value("${minio.local-dir:./uploads}") String localDir) {
        this.minio = minio;
        this.repo = repo;
        this.bucket = bucket;
        this.minioEnabled = minioEnabled;
        this.localDir = Paths.get(localDir).toAbsolutePath().normalize();
        log.info("文件存储模式: {}，本地目录: {}", minioEnabled ? "MinIO" : "本地磁盘", this.localDir);
    }

    public boolean isMinioEnabled() {
        return minioEnabled;
    }

    public Path localPath(String objectName) {
        return resolveSafe(objectName);
    }

    public AssetFileDto putObject(String objectName, String originalName, String contentType,
                                  long size, InputStream stream, Long ownerId) throws Exception {
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
            Path target = resolveSafe(objectName);
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
        AssetFileDto dto = toDto(repo.save(f));
        log.info("文件上传成功 objectName={} size={} owner={} mode={}",
                objectName, size, ownerId, minioEnabled ? "minio" : "local");
        return dto;
    }

    public String presignedGet(String objectName, int expireSeconds) throws Exception {
        if (!minioEnabled) {
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

    private AssetFileDto toDto(AssetFile f) {
        AssetFileDto dto = new AssetFileDto();
        dto.setId(f.getId());
        dto.setObjectName(f.getObjectName());
        dto.setBucket(f.getBucket());
        dto.setContentType(f.getContentType());
        dto.setSize(f.getSize());
        dto.setOwnerId(f.getOwnerId());
        dto.setCreatedAt(f.getCreatedAt());
        dto.setOriginalName(f.getOriginalName());
        return dto;
    }
}
