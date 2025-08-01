package com.example.jesse.item_market.market.impl;

import com.example.jesse.item_market.market.MarketService;
import com.example.jesse.item_market.market.exception.FundsNotEnough;
import com.example.jesse.item_market.market.exception.ItemNoOnMarket;
import com.example.jesse.item_market.market.exception.SelfTransactional;
import com.example.jesse.item_market.utils.LuaScriptReader;
import com.example.jesse.item_market.utils.dto.LuaOperatorResult;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

import static com.example.jesse.item_market.errorhandle.RedisErrorHandle.redisGenericErrorHandel;
import static com.example.jesse.item_market.utils.KeyConcat.*;
import static java.lang.String.format;

/** 市场交易操作实现类。*/
@Slf4j
@Service
public class MarketServiceImpl implements MarketService
{
    /** 专门为 LUA 脚本配置的 Redis 模板。*/
    @Autowired
    private
    ReactiveRedisTemplate<String, LuaOperatorResult> redisScriptTemplate;

    /** Lua 脚本读取工具。*/
    @Autowired
    private
    LuaScriptReader luaScriptReader;

    /** 组合 marketTransaction.lua 脚本需要的 KEYS，以列表形式返回。*/
    private @NotNull @Unmodifiable List<String>
    getTransactionKeys(String buyerId, String sellerId, String weaponId)
    {
        final String sellerUserKey      = getUserKey(sellerId);
        final String buyerUserKey       = getUserKey(buyerId);
        final String buyerInventoryKey  = getInventoryKey(buyerId);
        final String weaponHashKey      = getWeaponHashKey(weaponId);
        final String weaponZsetKey      = getWeaponPriceZsetKey();

        return List.of(
            sellerUserKey, buyerUserKey,
            buyerInventoryKey,
            weaponHashKey, weaponZsetKey
        );
    }

    /**
     * 市场交易执行方法实现。
     *
     * @param buyerId     买家 UUID
     * @param sellerId    卖家 UUID
     * @param weaponId    买家想购买的武器 UID
     *
     * @return 交易的整体操作是否完成
     */
    private @NotNull Mono<Void>
    executeTransaction(
        String buyerId, String sellerId, String weaponId)
    {
        return
        this.luaScriptReader
            .fromFile("marketTransaction.lua")
            .flatMap((script) ->
                this.redisScriptTemplate
                    .execute(
                        script,
                        this.getTransactionKeys(buyerId, sellerId, weaponId),
                        buyerId, sellerId, weaponId)
                    .timeout(Duration.ofSeconds(5L))
                    .next()
                    .flatMap((result) ->
                        switch (result.getResult())
                        {
                            case "SELF_TRANSACTIONAL" ->
                                Mono.error(
                                    new SelfTransactional(
                                        "Self transaction is forbidden!",
                                        null
                                    )
                                );

                            case "WEAPON_NOT_FOUND" ->
                                Mono.error(
                                    new ItemNoOnMarket(
                                        format(
                                            "[Transaction Buyer: %s -> Seller: %s] " +
                                            "Weapon: %s not exist in market!",
                                            buyerId, sellerId, weaponId
                                        ), null
                                    )
                                );

                            case "BUYER_FUNDS_NOT_FOUND" ->
                                Mono.error(
                                    new IllegalArgumentException(
                                        format(
                                            "[Transaction Buyer: %s -> Seller: %s] " +
                                            "Buyer funds not found!", buyerId, sellerId
                                        )
                                    )
                                );

                            case "BUYER_FUNDS_NOT_ENOUGH" ->
                                Mono.error(
                                  new FundsNotEnough(
                                      format(
                                          "[Transaction Buyer: %s -> Seller: %s] " +
                                          "Buyer funds not enough!",
                                          buyerId, sellerId
                                      ), null
                                  )
                                );

                            case "SUCCESS" -> Mono.empty();

                            case null, default ->
                                throw new IllegalStateException(
                                    "Unexpected value: " + result.getResult()
                                );
                        })
            );
    }

    /**
     * 市场交易开放接口。
     *
     * @param buyerId     买家 UUID
     * @param sellerId    卖家 UUID
     * @param weaponId    买家想购买的武器 UID
     *
     * @return 交易的整体操作是否完成
     */
    @Override
    public Mono<Void>
    marketTransaction(@NotNull String buyerId, String sellerId, String weaponId)
    {
        // 在脚本外检测是否左手倒右手
        if (buyerId.equals(sellerId))
        {
            return Mono.error(
                new SelfTransactional(
                    "Self transaction is forbidden!", null
                )
            );
        }

        return this.executeTransaction(buyerId, sellerId, weaponId)
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
