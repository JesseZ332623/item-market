package com.example.jesse.item_market.response.pojo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 分页信息元数据 POJO。*/
@Data
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class Pagination
{
    private int  pageNo;      // 第几页？
    private int  pageSize;    // 一页几条数据？
    private long totalItems;  // 总共几条数据？
}
