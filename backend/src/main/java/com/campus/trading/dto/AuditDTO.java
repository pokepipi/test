package com.campus.trading.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class AuditDTO {

    /** approve-通过 reject-驳回 */
    @NotBlank(message = "审核操作不能为空")
    private String action;

    /** 驳回原因 */
    private String reason;
}
