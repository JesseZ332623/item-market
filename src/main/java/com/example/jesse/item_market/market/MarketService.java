package com.example.jesse.item_market.market;

import reactor.core.publisher.Mono;

/** 市场交易操作接口类。*/
public interface MarketService
{
    /**
     * 市场交易开放接口。
     *
     * @param buyerId     买家 UUID
     * @param sellerId    卖家 UUID
     * @param weaponId    买家想购买的武器 UID
     *
     * @return 交易的整体操作是否完成
     */
    Mono<Void>
    marketTransaction(
        String buyerId, String sellerId, String weaponId);
}
