package com.swordshield.file.controller;

import com.swordshield.common.JwtUtil;
import com.swordshield.common.Response;
import com.swordshield.file.domain.AssetFile;
import com.swordshield.file.service.FileService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/file")
public class FileController {
    private final FileService service;
    public FileController(FileService service) { this.service = service; }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<AssetFile> upload(@RequestHeader("Authorization") String authorization,
                                      @RequestPart("file") MultipartFile file) throws Exception {
        String token = authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : null;
        Long userId = Long.valueOf(JwtUtil.parseSubject(token));
        
        // Generate unique object name: UUID + original extension
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectName = UUID.randomUUID().toString() + extension;
        
        AssetFile meta = service.putObject(objectName, originalFilename, file.getContentType(), file.getSize(), file.getInputStream(), userId);
        return Response.ok(meta);
    }

    @GetMapping("/presigned")
    public Response<String> presigned(@RequestParam String objectName, @RequestParam(defaultValue = "300") int expireSeconds) throws Exception {
        return Response.ok(service.presignedGet(objectName, expireSeconds));
    }

    /** 本地存储模式下的文件直读（生产切 MinIO 时由预签名地址替代）。 */
    @GetMapping("/raw")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> raw(
            @RequestParam String objectName) throws Exception {
        var path = service.localPath(objectName);
        var resource = new org.springframework.core.io.FileSystemResource(path);
        if (!resource.exists() || !resource.isReadable()) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        String contentType = java.net.URLConnection.guessContentTypeFromName(objectName);
        if (contentType == null) contentType = "application/octet-stream";
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Type", contentType)
                .header("Content-Disposition", "inline; filename=\"" + objectName + "\"")
                .body(resource);
    }
}

