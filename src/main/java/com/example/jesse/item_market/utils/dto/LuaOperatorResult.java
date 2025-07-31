package com.example.jesse.item_market.utils.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lua 脚本的执行结果是一个 Json 类型，会被映射成本 DTO。
 * 未来随着任务复杂度的提升，这个 DTO 也会随之扩展。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LuaOperatorResult
{
    private String result;
}
