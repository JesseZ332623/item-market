package com.example.jesse.item_market.email.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;

import static java.lang.String.format;

/** 验证码生成器工具类。*/
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class VerifyCodeGenerator
{
    /**
     * 生成长度为 digits 的验证码并返回。
     *
     * @param digits 验证码位数（不得为负）
     *
     * @throws IllegalArgumentException
     *         检查到 digits 为负或者超过最大位数时抛出
     */
    public static @NotNull Mono<String>
    generateVerifyCode(int digits)
    {
        if (digits <= 0 || digits >= 20)
        {
            return Mono.error(
                new IllegalArgumentException(
                    format(
                        "Number digits must be positive, your value = %d.",
                        digits
                    )
                )
            );
        }

        /*
         * 使用 Mono.fromSupplier() 操作封装同步的验证码生成逻辑，
         * 构建一个惰性执行的 Mono。
         */
        return Mono.fromSupplier(() -> {
                char[] buffer             = new char[digits];
                SecureRandom secureRandom = new SecureRandom();

                for (int index = 0; index < digits; index++)
                {
                    buffer[index]
                        = (char) ('0' + secureRandom.nextInt(10));
                }

                return String.valueOf(buffer);
            }
        );
    }
}
