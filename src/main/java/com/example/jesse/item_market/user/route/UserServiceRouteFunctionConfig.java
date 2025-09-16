package com.example.jesse.item_market.user.route;

import com.example.jesse.item_market.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static com.example.jesse.item_market.user.route.UserServiceURIConfig.*;

/** 用户服务路由函数配置类。*/
@Configuration
@RequiredArgsConstructor
public class UserServiceRouteFunctionConfig
{
    /** 用户服务接口 */
    private final UserService userService;

    @Bean
    public RouterFunction<ServerResponse>
    userServiceRouteFunction()
    {
        return
        RouterFunctions
            .route()
            .GET(GET_ALL_USER_UUIDS,              this.userService::findAllUserUUID)
            .GET(GET_USER_CONTACTS,               this.userService::findContactListByUUID)
            .GET(GET_USER_INVENTORY,              this.userService::findAllWeaponsFromInventoryByUUID)
            .GET(GET_USER_MARKET_LISTINGS,        this.userService::findAllWeaponsFromMarketByUUID)
            .GET(GET_USER_MARKET_LISTING_IDS,     this.userService::findAllWeaponIdsFromMarketByUUID)
            .GET(GET_USER_INFO,                   this.userService::findUserInfoByUUID)
            .POST(CREATE_USER,                    this.userService::createNewUser)
            .POST(ADD_CONTACT,                    this.userService::addNewContact)
            .POST(ADD_WEAPON_TO_INVENTORY,        this.userService::addWeaponToInventory)
            .POST(ADD_WEAPON_TO_MARKET,           this.userService::addWeaponToMarket)
            .DELETE(REMOVE_WEAPON_FROM_INVENTORY, this.userService::destroyWeaponFromInventory)
            .DELETE(REMOVE_CONTACT,               this.userService::removeContact)
            .DELETE(REMOVE_WEAPON_FROM_MARKET,    this.userService::removeWeaponFromMarket)
            .DELETE(DELETE_USER,                  this.userService::deleteUser)
            .build();
    }
}