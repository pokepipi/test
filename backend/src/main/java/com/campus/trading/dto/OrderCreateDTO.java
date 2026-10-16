package com.campus.trading.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class OrderCreateDTO {

    @NotNull(message = "物品不能为空")
    private Long itemId;

    /** 成交价, 支持议价后价格 */
    @NotNull(message = "成交价不能为空")
    @DecimalMin(value = "0.01", message = "成交价必须大于0")
    private BigDecimal price;

    @NotBlank(message = "请填写收货人")
    private String contactName;

    @NotBlank(message = "请填写联系电话")
    private String contactPhone;

    @NotBlank(message = "请填写收货地址")
    private String address;

    private String remark;
}
