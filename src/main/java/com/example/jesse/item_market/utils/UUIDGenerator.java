package com.example.jesse.item_market.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * 基于时间戳和随机数的 UUID 生成器，
 * UUID 以 long 类型给出，结构如下：
 * [time-stamp 48 bit][random-number 16 bit]
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class UUIDGenerator
{
    private static final SecureRandom random = new SecureRandom();

    /** 获取 long 类型的 UUID。*/
    public static long generateAsLong()
    {
        long timeStamp
            = Instant.now().toEpochMilli() & 0xFFFFFFFFFFFFL;

        long randomPart
            = random.nextLong() & 0xFFFF;

        return (timeStamp << 16) | randomPart;
    }

    /** 获取字符串类型的 UUID。*/
    public static @NotNull String
    generateAsSting() { return String.valueOf(generateAsLong()); }

    /** 获取 16 进制编码字符串类型的 UUID。*/
    public static @NotNull String
    generateAsHex() { return Long.toHexString(generateAsLong()); }
}
