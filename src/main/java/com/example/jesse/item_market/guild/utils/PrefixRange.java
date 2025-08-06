package com.example.jesse.item_market.guild.utils;

import lombok.*;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static java.lang.String.format;

/**
 * <p>
 *     对于一个元素非常多的列表（比如有几千个元素），
 *     还使用 LRANGE key 0 -1 查询所以元素再逐个遍历搜索的话，
 *     效率是非常慢的。</br>
 *     因此可以活用 Redis Sorted Set 数据结构的特性：
 *     当集合内所有成员的 score 皆为 0 时，成员会按照字符串的二进制顺序进行排序。
 *     此时只需要构建待搜索前缀的前驱（predecessor）和后继（successor），
 *     即可确定所有匹配前缀的用户名范围，届时再调用 ZRANGE 命令就可以轻松获取匹配的结果。
 * </p>
 *
 * <strong>此处还需要解释构建前驱和后继的规则，假设输入前缀："Je"</strong>
 *
 * <ul>
 *     <li>
 *         前驱：
 *         将给定前缀的最后一个字符替换为第一个排在该字符前面的字符。
 *         此外，为防止多个前缀搜索时出现问题，还需要给前缀的末尾拼接上左花括号，
 *         以便在需要时根据这个左花括号来过滤掉被插入到有序集合里面的起始和结束元素。
 *         结果是："Jd{"
 *     </li>
 *     <li>
 *         后继：
 *         将给定前缀的末尾拼接上左花括号，结果是："Je{"
 *     </li>
 * </ul>
 */
@Getter
@Accessors(chain = true)
@NoArgsConstructor(access  = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
final public class PrefixRange
{
    /** 可用的有效字符串。*/
    private final static
    String VALID_CHARS = "`-0123456789abcdefghijklmnopqrstuvwxyz{";

    /** 以有效字符为键，字符索引为值，构建哈希表，避免重复的线性搜索影响性能。*/
    private final static
    Map<Character, Integer> CHAR_POSITIONS;

    static
    {
        CHAR_POSITIONS = new HashMap<>();

        for (int index = 0; index < VALID_CHARS.length(); ++index) {
            CHAR_POSITIONS.put(VALID_CHARS.charAt(index), index);
        }
    }

    private String predecessor;
    private String successor;

    /**
     * 给定前缀 prefix，构建它的前驱与后继。
     *
     * @param prefix 给定的前缀字符串
     *
     * @return 发布 PrefixRange 的 Mono
     *
     * @throws IllegalArgumentException 当 prefix 为 empty 或者存在无效字符时抛出
     */
    @Contract("_ -> new")
    public static @NotNull Mono<PrefixRange>
    create(@NotNull String prefix)
    {
        return
        Mono.fromCallable(() -> {
            if (prefix.isEmpty()) {
                throw new IllegalArgumentException("Param prefix can't be empty");
            }

            String lowerCasePrefix = prefix.toLowerCase(Locale.ROOT);
            int prefixLength       = lowerCasePrefix.length();

            // 找出 prefix 的最后一个字符在 validChars 中的位置
            char lastChar    = lowerCasePrefix.charAt(prefixLength - 1);
            Integer position = CHAR_POSITIONS.get(lastChar);

            // 注意检查非法字符
            if (position == null)
            {
                throw new IllegalArgumentException(
                    format(
                        "Illegal character %c in prefix: %s!",
                        lastChar, prefix
                    )
                );
            }

            String predecessor;

            // 处理最小字符边界
            if (position == 0) { predecessor = "{" + "{"; }
            else
            {
                // 获取给定前缀的最后一个字符的第一个排在该字符前面的字符
                char prevChar
                    = VALID_CHARS.charAt(position - 1);

                // 构建前驱
                predecessor
                    = lowerCasePrefix.substring(0, prefixLength - 1) + prevChar + "{";
            }

            // 构建后继
            String successor = lowerCasePrefix + "{";

            return new PrefixRange(predecessor, successor);
        })
        .onErrorMap(
            IllegalArgumentException.class,
            (exception) ->
                new CreatePrefixRangeFailed(exception.getMessage(), exception)
        );
    }
}