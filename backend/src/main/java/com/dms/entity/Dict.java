package com.dms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_dict")
public class Dict {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String dictType;
    private String dictCode;
    private String dictValue;
}
