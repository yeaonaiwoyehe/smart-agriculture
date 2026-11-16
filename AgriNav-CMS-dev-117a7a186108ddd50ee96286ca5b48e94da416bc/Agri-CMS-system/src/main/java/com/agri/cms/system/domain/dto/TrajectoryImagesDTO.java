package com.agri.cms.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

/**
 * @author : simon
 * @description :
 * @last : 2024-12-09 15:21
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-12-09 15:21
 */
public class TrajectoryImagesDTO {
    @Pattern(regexp = "^[0-9]{11}$", message = "手机号格式不正确")
    @NotBlank(message = "手机号不能为空")
    private String phoneNumber;


    private LocalDateTime startTime;

}
