package com.campus.trading.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Data
public class PublishItemDTO {

    @NotNull(message = "请选择分类")
    private Long categoryId;

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最长100个字符")
    private String title;

    @Size(max = 2000, message = "描述最长2000个字符")
    private String description;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    private BigDecimal price;

    /** 图片URL, 逗号分隔 */
    private String images;
}
