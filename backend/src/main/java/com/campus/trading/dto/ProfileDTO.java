package com.campus.trading.dto;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class ProfileDTO {

    @Size(max = 20, message = "昵称最长20个字符")
    private String nickname;

    private String phone;

    private String email;

    private String avatar;
}
