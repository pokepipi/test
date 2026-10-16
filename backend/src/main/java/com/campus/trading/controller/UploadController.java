package com.campus.trading.controller;

import com.campus.trading.common.BusinessException;
import com.campus.trading.common.Result;
import com.campus.trading.util.ImageUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件上传接口 (图片自动压缩)
 */
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Value("${upload.dir}")
    private String uploadDir;

    @PostMapping("/image")
    public Result<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择图片文件");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException(400, "图片大小不能超过10MB");
        }
        String url = ImageUtil.saveImage(file, uploadDir);
        return Result.ok(url);
    }
}
