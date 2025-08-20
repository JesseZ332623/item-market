package com.example.jesse.item_market.location_search;

import reactor.core.publisher.Mono;

/** 地理信息相关 CSV 文件读取并存储至 Redis 接口。*/
public interface GeoLiteCityReader
{
    /**
     * （测试时用）
     * 从文件系统中读取 GeoLiteCity-Blocks.csv 文件，
     * 将所有数据处理并保存到 Redis 中去，Redis 数据设计如下：
     *
     * <ul>
     *     <li> Key: location:ip_to_cityid</li>
     *     <li> Type: ZSET</li>
     *     <li> Member: cityid_uuid</li>
     *     <li> Score: start_ip_num</li>
     * </ul>
     */
    Mono<Void> readBlocksFromFile();

    /**
     * （生产环境用）
     * 从文件系统中读取 GeoLiteCity-Blocks.csv 文件，
     * 将所有数据处理并保存到 Redis 中去，Redis 数据设计如下：
     *
     * <ul>
     *     <li> Key: location:ip_to_cityid</li>
     *     <li> Type: ZSET</li>
     *     <li> Member: cityid_uuid</li>
     *     <li> Score: start_ip_num</li>
     * </ul>
     */
    Mono<Void> readBlocksFromClassPath();

    /**
     * （测试时用）
     * 读取 GeoLiteCity-Location.csv 文件，
     * 将所有数据处理并保存到 Redis 中去，Redis 数据设计如下：
     *
     * <ul>
     *     <li>Key: location:cityid_to_cityname</li>
     *     <li>Type: Hash</li>
     *     <li>Hash-Key: cityid</li>
     *     <li>
     *         Hash-Value: (JSON)
     *         {country, region, city, postalCode, latitude, longitude, metroCode, areaCode}
     *     </li>
     * </ul>
     */
    Mono<Void> readLocationFromFile();

    /**
     * （生产环境用）
     * 读取 GeoLiteCity-Location.csv 文件，
     * 将所有数据处理并保存到 Redis 中去，Redis 数据设计如下：
     *
     * <ul>
     *     <li>Key: location:cityid_to_cityname</li>
     *     <li>Type: Hash</li>
     *     <li>Hash-Key: cityid</li>
     *     <li>
     *         Hash-Value: (JSON)
     *         {country, region, city, postalCode, latitude, longitude, metroCode, areaCode}
     *     </li>
     * </ul>
     */
    Mono<Void> readLocationFromClassPath();
}
