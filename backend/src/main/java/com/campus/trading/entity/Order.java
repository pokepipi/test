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
 * 订单实体
 */
@Data
@TableName("orders")
public class Order {

    public static final int STATUS_UNPAID = 0;     // 待付款
    public static final int STATUS_UNSHIPPED = 1;  // 待发货
    public static final int STATUS_UNRECEIVED = 2; // 待收货
    public static final int STATUS_FINISHED = 3;   // 已完成
    public static final int STATUS_CANCELED = 4;   // 已取消

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long itemId;

    private Long buyerId;

    private Long sellerId;

    private BigDecimal price;

    private Integer status;

    private String contactName;

    private String contactPhone;

    private String address;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime shipTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 展示用(不入库) */
    @TableField(exist = false)
    private String itemTitle;

    @TableField(exist = false)
    private String itemImage;

    @TableField(exist = false)
    private String buyerName;

    @TableField(exist = false)
    private String sellerName;
}
