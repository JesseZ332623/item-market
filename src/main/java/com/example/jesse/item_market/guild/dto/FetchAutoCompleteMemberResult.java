package com.example.jesse.item_market.guild.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 表示自动补全结果的 DTO。*/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FetchAutoCompleteMemberResult
{
    private List<String> matchedMembers;
}
