package com.example.jesse.item_market.location_search.dto;

import com.example.jesse.item_market.location_search.uitls.Ipv4Converter;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * 由 GeoLiteCity-Blocks.csv 文件映射而来的，
 * 表示 IPV4 地址和位置 ID 映射关系的 DTO。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode
@NoArgsConstructor(access  = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class IpToCityID
{
    private String localId;
    private double startIpScore;

    @Override
    public String toString()
    {
        return "[" +
                    "Start IP: " + Ipv4Converter.intToIPv4((int) this.startIpScore) +
                    " Local ID: " + this.localId
                + "]";
    }
}
