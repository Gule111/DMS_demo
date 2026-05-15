package com.dms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dms.entity.Dict;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface DictMapper extends BaseMapper<Dict> {
    
    @Select("SELECT * FROM sys_dict WHERE dict_type = #{type}")
    List<Dict> selectByType(String type);
}
