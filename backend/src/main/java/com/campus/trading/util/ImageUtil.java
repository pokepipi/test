package com.campus.trading.util;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * 图片上传与压缩工具
 *  - 超过 1200px 宽自动等比压缩并降低质量, 大幅节省存储与流量
 *  - GIF 动图/无法解析的图片原样保存
 */
public class ImageUtil {

    private static final int MAX_WIDTH = 1200;
    private static final double QUALITY = 0.85;

    /**
     * 保存图片并返回访问 URL
     *
     * @param file    上传文件
     * @param baseDir 上传根目录
     * @return 形如 /uploads/20240901/xxxx.jpg 的访问路径
     */
    public static String saveImage(MultipartFile file, String baseDir) throws IOException {
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase()
                : "jpg";

        String subDir = new SimpleDateFormat("yyyyMMdd").format(new Date());
        File dir = new File(baseDir, subDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("创建上传目录失败: " + dir.getAbsolutePath());
        }

        String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        File target = new File(dir, name);

        if ("gif".equals(ext)) {
            // 动图不做压缩, 原样保存
            file.transferTo(target);
        } else {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                file.transferTo(target);
            } else if (image.getWidth() > MAX_WIDTH) {
                Thumbnails.of(image)
                        .width(MAX_WIDTH)
                        .outputQuality(QUALITY)
                        .toFile(target);
            } else {
                String format = "png".equals(ext) ? "png" : "jpg";
                ImageIO.write(image, format, target);
            }
        }
        return "/uploads/" + subDir + "/" + name;
    }
}
