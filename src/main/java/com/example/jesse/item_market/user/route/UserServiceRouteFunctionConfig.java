package com.example.jesse.item_market.user.route;

import com.example.jesse.item_market.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static com.example.jesse.item_market.user.route.UserServiceURIConfig.*;

/** 用户服务路由函数配置类。*/
@Configuration
public class UserServiceRouteFunctionConfig
{
    @Autowired
    private UserService userService;

    @Bean
    public RouterFunction<ServerResponse>
    userServiceRouteFunction()
    {
        return
        RouterFunctions
            .route()
            .GET(FIND_ALL_USER_UUID_URI,              this.userService::findAllUserUUID)
            .GET(FIND_CONTACTS_BY_UUID_URI,           this.userService::findContactListByUUID)
            .GET(FIND_ALL_WEAPONS_FROM_INVENTORY_URI, this.userService::findAllWeaponsFromInventoryByUUID)
            .GET(FIND_ALL_WEAPONS_FROM_MARKET_URI,    this.userService::findAllWeaponsFromMarketByUUID)
            .GET(FIND_ALL_WEAPONIDS_FROM_MARKET_URI,  this.userService::findAllWeaponIdsFromMarketByUUID)
            .GET(FIND_USER_INFO_URI,                  this.userService::findUserInfoByUUID)
            .POST(CREATE_NEW_USER_URI,                this.userService::createNewUser)
            .POST(ADD_NEW_CONTACT_URI,                this.userService::addNewContact)
            .POST(ADD_WEAPON_TO_INVENTORY_URI,        this.userService::addWeaponToInventory)
            .POST(ADD_WEAPON_TO_MARKET_URI,           this.userService::addWeaponToMarket)
            .DELETE(DESTORY_WEAPON_FROM_INVENTORY,    this.userService::destroyWeaponFromInventory)
            .DELETE(REMOVE_CONTACT_URI,               this.userService::removeContact)
            .DELETE(REMOVE_WEAPON_FROM_MARKET,        this.userService::removeWeaponFromMarket)
            .DELETE(DELETE_USER,                      this.userService::deleteUser)
            .build();
    }
}