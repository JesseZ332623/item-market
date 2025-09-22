package com.example.jesse.item_market.response.pojo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;

/** 表示 HATEOAS 元数据中的某一条链接的 POJO。*/
@Data
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class Link
{
    private String      rel;
    private String      href;
    private HttpMethod  method;
}
