package com.example.jesse.item_market.guild.utils;

import lombok.*;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 对于一个元素非常多的列表（比如有几千个元素），
 * 还使用 LRANGE key 0 -1 查询所以元素再逐个遍历搜索的话，
 * 效率是非常慢的。</br>
 * 因此可以活用 Redis Sorted Set 数据结构的特性：
 * 当集合内所有成员的 score 皆为 0 时，成员会按照字符串的二进制顺序进行排序。
 * 此时只需要构建待搜索前缀的前驱（predecessor）和后继（successor），
 * 即可确定所有匹配前缀的用户名范围，
 * 届时再调用 ZRANGEBYLEX 命令就可以轻松获取匹配的结果
 * （这个命令需要 Redis 版本 >= 2.8.9，Spring Data Redis 版本 >= 2.3.0）。
 * </p>
 */
@Getter
@Accessors(chain = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class PrefixRange
{
    private static final String ESCAPED_QUOTE = "\"";
    private static final String LEFT_BRACKET  = "[";
    private static final String PLUS          = "+";
    private static final String MINUS         = "-";
    private static final String VALID_CHARS   = "/0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private static final Map<Character, Integer> CHAR_POSITIONS;

    static {
        // Validate character order
        for (int i = 1; i < VALID_CHARS.length(); i++)
        {
            if (VALID_CHARS.charAt(i) <= VALID_CHARS.charAt(i - 1))
            {
                throw new IllegalStateException("VALID_CHARS must be in ascending order!");
            }
        }

        CHAR_POSITIONS = new HashMap<>();

        for (int i = 0; i < VALID_CHARS.length(); i++)
        {
            CHAR_POSITIONS.put(VALID_CHARS.charAt(i), i);
        }
    }

    private String min;  // Inclusive lower bound
    private String max;  // Exclusive upper bound

    public static @NotNull
    Mono<PrefixRange> create(String prefix)
    {
        return Mono.fromCallable(() -> {
            // Parameter validation
            if (prefix == null || prefix.isEmpty()) {
                throw new IllegalArgumentException("Prefix cannot be null or empty");
            }

            // Check for special characters that might break Redis lex commands
            if (prefix.contains("\"") || prefix.contains("\\") || prefix.contains("[") || prefix.contains("(")) {
                throw new IllegalArgumentException("Prefix contains special characters");
            }

            // Build the inclusive lower bound - matches "\"prefix"
            String min = LEFT_BRACKET + ESCAPED_QUOTE + prefix;

            // Build the exclusive upper bound
            String max;
            char lastChar = prefix.charAt(prefix.length() - 1);
            Integer position = CHAR_POSITIONS.get(lastChar);

            if (position == null)
            {
                throw new IllegalArgumentException(
                    String.format("Illegal character '%c' in prefix: %s", lastChar, prefix)
                );
            }

            if (position == VALID_CHARS.length() - 1) {
                max = PLUS; // +inf
            }
            else
            {
                char nextChar = VALID_CHARS.charAt(position + 1);
                String base = prefix.substring(0, prefix.length() - 1);
                // Matches "\"base<nextChar>"
                max = LEFT_BRACKET + ESCAPED_QUOTE + base + nextChar;
            }

            return new PrefixRange(min, max);
        }).onErrorMap(IllegalArgumentException.class,
            e -> new CreatePrefixRangeFailed(e.getMessage(), e)
        );
    }
}