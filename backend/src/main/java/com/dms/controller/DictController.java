package com.dms.controller;

import com.dms.common.Result;
import com.dms.entity.Dict;
import com.dms.mapper.DictMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dict")
public class DictController {

    private final DictMapper dictMapper;

    public DictController(DictMapper dictMapper) {
        this.dictMapper = dictMapper;
    }

    @GetMapping("/type/{type}")
    public Result<List<Dict>> getByType(@PathVariable String type) {
        return Result.success(dictMapper.selectByType(type));
    }

    @GetMapping("/all")
    public Result<List<Dict>> getAll() {
        return Result.success(dictMapper.selectList(null));
    }

    @PostMapping("/save")
    public Result<String> save(@RequestBody Dict dict) {
        if (dict.getId() == null) {
            dictMapper.insert(dict);
        } else {
            dictMapper.updateById(dict);
        }
        return Result.success("保存成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        dictMapper.deleteById(id);
        return Result.success("删除成功");
    }
}
