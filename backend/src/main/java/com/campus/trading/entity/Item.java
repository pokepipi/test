package com.campus.trading.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 二手物品实体
 */
@Data
@TableName("item")
public class Item {

    public static final int STATUS_PENDING = 0;   // 待审核
    public static final int STATUS_ON_SALE = 1;   // 在售
    public static final int STATUS_OFF = 2;       // 已下架
    public static final int STATUS_SOLD = 3;      // 已售出
    public static final int STATUS_REJECTED = 4;  // 审核驳回

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long categoryId;

    private String title;

    private String description;

    private BigDecimal price;

    /** 图片URL, 逗号分隔 */
    private String images;

    private Integer status;

    private Integer viewCount;

    /** 发布时是否命中敏感词 */
    private Integer sensitiveHit;

    private String rejectReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 展示用(不入库) */
    @TableField(exist = false)
    private String sellerName;

    @TableField(exist = false)
    private String sellerAvatar;

    @TableField(exist = false)
    private String categoryName;
}
