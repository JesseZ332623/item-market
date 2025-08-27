package com.example.jesse.item_market.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;

/**
 * 这个类用来表示 HATEOAS 元数据中的某一条链接。
 */
@Data
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class Link
{
    private String      rel;
    private String      href;
    private HttpMethod  method;
}
