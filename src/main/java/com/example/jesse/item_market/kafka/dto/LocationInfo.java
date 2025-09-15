package com.example.jesse.item_market.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.Accessors;

/** 由 GeoLiteCity-Location.csv 文件映射而来的，表示详细地理信息的 DTO。*/
@Data
@ToString
@Accessors(chain = true)
@EqualsAndHashCode
@NoArgsConstructor(access  = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@JsonIgnoreProperties(ignoreUnknown = true)       // 忽略 JSON 中的未知字段
public class LocationInfo
{
    private String locationId;  // 城市 ID（该字段不需要序列化）
    private String country;     // 国名
    private String region;      // 地区名
    private String city;        // 城市名
    private String postalCode;  // 城市邮政编码
    private double latitude;    // 经度
    private double longitude;   // 纬度
    private int    metroCode;   // 地铁号
    private int    areaCode;    // 地区代码
}