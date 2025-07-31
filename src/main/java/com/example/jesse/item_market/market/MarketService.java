package com.example.jesse.item_market.market;

import com.example.jesse.item_market.utils.LuaScriptReader;
import com.example.jesse.item_market.utils.dto.LuaOperatorResult;
import com.example.jesse.item_market.market.exception.FundsNotEnough;
import com.example.jesse.item_market.market.exception.ItemNoOnMarket;
import com.example.jesse.item_market.market.exception.SelfTransactional;
import com.example.jesse.item_market.user.Weapons;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static com.example.jesse.item_market.errorhandle.RedisErrorHandle.redisGenericErrorHandel;

/** 市场交易操作实现类。（由于 Redis 中市场相关数据结构变更，这个类暂未实现） */
@Slf4j
@Service
public class MarketService
{
    @Autowired
    private
    ReactiveRedisTemplate<String, LuaOperatorResult> redisScriptTemplate;

    @Autowired
    private
    LuaScriptReader luaScriptReader;

    /**
     * 市场交易执行方法实现。
     *
     * @param buyerId     买家 UUID
     * @param sellerId    卖家 UUID
     * @param weapon      买家想购买的武器
     *
     * @return 交易的整体操作是否完成
     */
    private @NotNull Mono<Void>
    executeTransaction(
        @NotNull String buyerId, String sellerId, Weapons weapon
    )
    {
        if (buyerId.equals(sellerId))
        {
            return Mono.error(
                new SelfTransactional(
                    "Self transaction is forbident!", null
                )
            );
        }

        // 明天实现。
        return Mono.empty();
    }

    /**
     * 市场交易开放接口。
     *
     * @param buyerId     买家 UUID
     * @param sellerId    卖家 UUID
     * @param weapon      买家想购买的武器
     *
     * @return 交易的整体操作是否完成
     */
    public Mono<Void>
    marketTransaction(String buyerId, String sellerId, Weapons weapon)
    {
        return this.executeTransaction(buyerId, sellerId, weapon)
                   .timeout(Duration.ofSeconds(5L))
                   .onErrorResume((exception) -> {
                        switch (exception)
                        {
                            case FundsNotEnough fundsNotEnough ->
                            {
                                log.info("{}", fundsNotEnough.getMessage());
                                return Mono.empty();
                            }

                            case ItemNoOnMarket itemNoOnMarket ->
                            {
                                log.info("{}", itemNoOnMarket.getMessage());
                                return Mono.empty();
                            }

                            default -> {
                                return redisGenericErrorHandel(
                                    exception, null
                                );
                            }
                        }
                   });
    }
}
