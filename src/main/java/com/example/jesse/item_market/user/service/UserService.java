package com.example.jesse.item_market.user.service;

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/** 用户服务接口。*/
public interface UserService
{
    /** 获取所有用户的 UUID。*/
    Mono<ServerResponse>
    findAllUserUUID(ServerRequest request);

    /** 获取某个用户的最近联系人列表。*/
    Mono<ServerResponse>
    findContactListByUUID(ServerRequest request);

    /** 获取某个用户包裹中的所有武器。*/
    Mono<ServerResponse>
    findAllWeaponsFromInventoryByUUID(ServerRequest request);

    /** 获取某个用户上架至市场的所有武器。*/
    Mono<ServerResponse>
    findAllWeaponsFromMarketByUUID(ServerRequest request);

    /** 获取某个用户上架至市场的所有武器的 ID。*/
    Mono<ServerResponse>
    findAllWeaponIdsFromMarketByUUID(ServerRequest request);

    /** 获取某个用户的数据。*/
    Mono<ServerResponse>
    findUserInfoByUUID(ServerRequest request);

    /** 创建一个新用户，并为它随机挑选几件武器放入包裹。*/
    Mono<ServerResponse>
    createNewUser(ServerRequest request);

    /** 用户记录另一个用户为最近联系人。*/
    Mono<ServerResponse>
    addNewContact(ServerRequest request);

    /** 用户删除自己最近联系人列表的某个用户。*/
    Mono<ServerResponse>
    removeContact(ServerRequest request);

    /** 为用户的包裹添加一件武器。*/
    Mono<ServerResponse>
    addWeaponToInventory(ServerRequest request);

    /** 用户销毁包裹中的某个武器。*/
    Mono<ServerResponse>
    destroyWeaponFromInventory(ServerRequest request);

    /** 用户将武器上架至市场。*/
    Mono<ServerResponse>
    addWeaponToMarket(ServerRequest request);

    /** 用户从市场上下架某个武器。*/
    Mono<ServerResponse>
    removeWeaponFromMarket(ServerRequest request);

    /** 删除用户。*/
    Mono<ServerResponse>
    deleteUser(ServerRequest request);
}