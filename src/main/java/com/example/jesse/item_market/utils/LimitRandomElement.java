package com.example.jesse.item_market.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/** 获取列表随机数量且不重复的子列表工具类。*/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class LimitRandomElement
{
    /** 从列表中随机取 limit 个元素，并返回一个不重复的集合。*/
    public static <T> List<T>
    getRandomLimit(@NotNull List<T> list, long limit)
    {
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Param list not be empty!");
        }

        if (limit < 0) {
            throw new IllegalArgumentException("Param limit not less then 0!");
        }

        if (limit == 0) {
            return Collections.emptyList();
        }

        if (list.size() == limit || list.size() < limit) {
            return new ArrayList<>(list);
        }

        return ThreadLocalRandom
            .current()
            .ints(0, list.size())
            .distinct()
            .limit(limit)
            .mapToObj(list::get)
            .collect(Collectors.toCollection(ArrayList::new));
    }
}
