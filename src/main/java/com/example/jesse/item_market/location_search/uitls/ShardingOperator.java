package com.example.jesse.item_market.location_search.uitls;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** 分片操作工具类。*/
public class ShardingOperator
{
    /** 以城市 ID 为成员，IPv4 地址为分数的有序集合键。*/
    public static final String blocksRedisKey
        = "location:ip_to_cityid";

    /** 以城市 ID 为哈希键，对应的城市详细地理信息为哈希值的散列表键。*/
    public static final String locationRedisKey
        = "location:cityid_to_locationinfo";

    /**
     * 每个哈希分片最大键值对数，
     * 在这个范围内，Redis 使用 listpack 存储数据。
     */
    private static final int HASH_MAX_LISTPACK_ENTRYS = 1024;

    /**
     * 计算所需的分片数，对数据的规模要有前瞻性，
     * 假设在未来数据量会从约 90 万条涨到约 150 万条，基于 150 万条计算分片数。
     * */
    private static final int TOTAL_BUKETS
        = (int) Math.ceil(1500000.00 / HASH_MAX_LISTPACK_ENTRYS);

    /**
     * 将整数的 hash key 映射到某个分片，并返回该分片的键。
     *
     * @param hashKey  数字 hash-key
     *
     * @return 组合出来的完整分片 redis-key
     */
    @Contract(pure = true)
    public static @NotNull String
    getShardingKey(int hashKey)
    {
        return
        locationRedisKey + ":" + (hashKey % TOTAL_BUKETS);
    }
}