package com.example.jesse.item_market.location_search.impl;

import com.example.jesse.item_market.location_search.LocationSearcher;
import com.example.jesse.item_market.location_search.dto.LocationInfo;
import com.example.jesse.item_market.location_search.uitls.Ipv4Converter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.Objects;

import static com.example.jesse.item_market.errorhandle.RedisErrorHandle.redisGenericErrorHandel;
import static java.lang.String.format;

/** 通过 IP 查询具体地理信息的实现。*/
@Slf4j
@Service
public class LocationSearcherImpl implements LocationSearcher
{
    /** 以城市 ID 为成员，IPv4 地址为分数的有序集合键。*/
    private static final String blocksRedisKey
        = "location:ip_to_cityid";

    /** 以城市 ID 为哈希键，对应的城市详细地理信息为哈希值的散列表键。*/
    private static final String locationRedisKey
        = "location:cityid_to_locationinfo";

    /** 字符串序列化 Redis 模板。*/
    @Autowired
    private
    ReactiveRedisTemplate<String, String> stringRedisTemplate;

    /** 通用 Redis 模板。*/
    @Autowired
    private
    ReactiveRedisTemplate<String, Object> redisTemplate;

    /**
     * 在某个命令查询不到结果时，
     * 构造一个发布异常信息的 Mono。
     *
     * @param <T> 为下游链式调用准备的类型信息
     *
     * @param command Redis 查询命令 + 参数
     *               （如：ZREVRANGEBYSCORE location:ip_to_cityid 3758095360 3758095360）
     *
     * @throws NullPointerException 当参数 command 为 null 时
     *
     * @return 立即发出 {@link NoSuchElementException} 错误信号的 {@link Mono}
     */
    private <T> @NotNull Mono<T>
    buildNotFoundError(@NotNull String command)
    {
        Objects.requireNonNull(
            command,
            "Param command not be null!"
        );

        return
        Mono.error(
            new NoSuchElementException(
                format(
                    "Result of command: %s not exist!", command
                )
            )
        );
    }

    /**
     * 根据传入的 IPV4 地址，查询这个 IP 对应的详细地理信息。
     *
     * @param ipv4 IPv4 地址字符串（比如 192.168.1.2）
     *
     * @return 这个登录 IP 对应的详细地理位置 {@link LocationInfo}
     */
    @Override
    public Mono<LocationInfo>
    findLocationInfoByIpv4(String ipv4)
    {
        final double ipNumber
            = (double) Ipv4Converter.ipv4ToInt(ipv4);

        return
        this.stringRedisTemplate
            .opsForZSet()
            .reverseRangeByScore(blocksRedisKey, Range.closed(ipNumber, ipNumber))
            .switchIfEmpty(
                this.buildNotFoundError(
                    format(
                        "ZREVRANGEBYSCORE %s %.0f %.0f",
                        blocksRedisKey, ipNumber, ipNumber)))
            .timeout(Duration.ofSeconds(3L))
            .next()
            .map((res) ->
                res.split("_")[0].trim())
            .flatMap((locationId) ->
                this.redisTemplate
                    .opsForHash()
                    .get(locationRedisKey, locationId)
                    .switchIfEmpty(
                        this.buildNotFoundError(
                            format(
                                "HGET %s %s",
                                locationRedisKey, locationId)))
                    .timeout(Duration.ofSeconds(3L))
                    .map((res) -> (LocationInfo) res))
        .onErrorResume((exception) ->
            redisGenericErrorHandel(exception, null));
    }
}