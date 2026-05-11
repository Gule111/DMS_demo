package com.dms.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserRoleDTO {
    private Long id;
    private String username;
    private String phone;
    private Integer status;
    private LocalDateTime createdAt;
    
    private Long roleId;
    private String roleName;
    private String roleCode;
}
