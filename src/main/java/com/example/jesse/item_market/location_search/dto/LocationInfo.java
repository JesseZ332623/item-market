package com.example.jesse.item_market.location_search.dto;

import lombok.*;
import lombok.experimental.Accessors;

/** 由 GeoLiteCity-Location.csv 文件映射而来的，表示详细地理信息的 DTO。*/
@Data
@ToString
@Accessors(chain = true)
@EqualsAndHashCode
@NoArgsConstructor(access  = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LocationInfo
{
    private String country;     // 国名
    private String region;      // 地区名
    private String city;        // 城市名
    private String postalCode;  // 城市邮政编码
    private double latitude;    // 经度
    private double longitude;   // 纬度
    private int    metroCode;   // 地铁号
    private int    areaCode;    // 地区代码
}
