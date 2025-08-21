package com.example.jesse.item_market.location_search;

import com.example.jesse.item_market.location_search.dto.LocationInfo;
import reactor.core.publisher.Mono;

/** 通过 IP 查询具体地理信息的接口。*/
public interface LocationSearcher
{
    /**
     * 根据传入的 IPV4 地址，查询这个 IP 对应的详细地理信息。
     *
     * @param ipv4 IPv4 地址字符串（比如 192.168.1.2）
     *
     * @return 这个登录 IP 对应的详细地理位置 {@link LocationInfo}
     */
    Mono<LocationInfo>
    findLocationInfoByIpv4(String ipv4);
}
