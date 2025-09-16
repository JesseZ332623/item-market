package com.example.jesse.item_market.user.service.impl;

import com.example.jesse.item_market.response.ResponseBuilder;
import com.example.jesse.item_market.user.UserRedisService;
import com.example.jesse.item_market.user.Weapons;
import com.example.jesse.item_market.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

import static com.example.jesse.item_market.response.URLParamPrase.*;

/** 用户服务实现。*/
@Slf4j
@Service
public class UserServiceImpl implements UserService
{
    @Autowired
    private UserRedisService userRedisService;

    @Autowired
    private ResponseBuilder responseBuilder;

    /** 获取所有用户的 UUID。*/
    @Override
    public Mono<ServerResponse>
    findAllUserUUID(ServerRequest request)
    {
        return
        this.userRedisService
            .getAllUserUUID().collectList()
            .flatMap((uuids) -> {
                if (uuids.isEmpty()) {
                    return
                    this.responseBuilder
                        .NOT_FOUND("We don't have any users!", null);
                }

                return
                this.responseBuilder
                    .OK(
                        uuids, String.format("OK! Find %d uuids.", uuids.size()),
                        null, null
                    );
            })
            .onErrorResume(
                IllegalArgumentException.class,
                (e) ->
                    this.responseBuilder
                        .BAD_REQUEST(e.getMessage(), e)
            )
            .onErrorResume((exception) ->
                this.responseBuilder
                    .INTERNAL_SERVER_ERROR("Find all users uuid failed!", exception)
            );
    }

    /** 获取某个用户的最近联系人列表。*/
    @Override
    public Mono<ServerResponse>
    findContactListByUUID(ServerRequest request)
    {
        return
        prasePathVariable(request, "userId")
            .flatMap((uuid) ->
                this.userRedisService
                    .getContactListByUUID(uuid)
                    .collectList()
                    .flatMap((contacts) -> {
                        if (contacts.isEmpty())
                        {
                            return
                            this.responseBuilder
                                .NOT_FOUND(
                                    String.format("User: %s don't have any contacts.", uuid),
                                    null
                                );
                        }

                        return
                        this.responseBuilder.OK(
                            contacts, String.format("Find %d contacts.", contacts.size()),
                            null, null
                        );
                    })
                    .onErrorResume((exception) ->
                        this.responseBuilder
                            .INTERNAL_SERVER_ERROR(
                                String.format(
                                    "Find contacts for user: %s failed!", uuid),
                                exception
                            )
                    ))
            .onErrorResume(
                IllegalArgumentException.class,
                (e) ->
                    this.responseBuilder
                        .BAD_REQUEST(e.getMessage(), e)
            );
    }

    /** 获取某个用户包裹中的所有武器。*/
    @Override
    public Mono<ServerResponse>
    findAllWeaponsFromInventoryByUUID(ServerRequest request)
    {
        return
        prasePathVariable(request, "userId")
            .flatMap((uuid) ->
                this.userRedisService
                    .getAllWeaponsFromInventoryByUUID(uuid)
                    .collectList()
                    .flatMap((weapons) -> {
                        if (weapons.isEmpty())
                        {
                            return
                            this.responseBuilder
                                .NOT_FOUND(
                                    String.format("User: %s don't have any weapons in inventory.", uuid),
                                    null
                                );
                        }

                        return
                        this.responseBuilder
                            .OK(
                                weapons,
                                String.format("Find %s weapons in inventory.", weapons.size()),
                                null, null
                            );
                    })
                    .onErrorResume((exception) ->
                        this.responseBuilder
                            .INTERNAL_SERVER_ERROR(
                                String.format(
                                    "Find weapons from inventory for user: %s failed!", uuid),
                                exception
                            )
                    )
            )
            .onErrorResume(
                IllegalArgumentException.class,
                (e) ->
                    this.responseBuilder
                        .BAD_REQUEST(e.getMessage(), e)
            );
    }

    /** 获取某个用户上架至市场的所有武器。*/
    @Override
    public Mono<ServerResponse>
    findAllWeaponsFromMarketByUUID(ServerRequest request)
    {
        return
        prasePathVariable(request, "userId")
            .flatMap((uuid) ->
                this.userRedisService
                    .getAllWeaponsFromMarketByUUID(uuid)
                    .collectList()
                    .flatMap((weapons) -> {
                        if (weapons.isEmpty())
                        {
                            return
                            this.responseBuilder
                                .NOT_FOUND(
                                    String.format("User: %s don't selling any weapons in market.", uuid),
                                    null
                                );
                        }

                        return
                        this.responseBuilder
                            .OK(
                                weapons,
                                String.format("Find %s weapons in market.", weapons.size()),
                                null, null
                            );
                    })
                    .onErrorResume((exception) ->
                        this.responseBuilder
                            .INTERNAL_SERVER_ERROR(
                                String.format(
                                    "Find weapons from market for user: %s failed!", uuid),
                                exception
                            )
                    )
            )
            .onErrorResume(
                IllegalArgumentException.class,
                (e) ->
                    this.responseBuilder
                        .BAD_REQUEST(e.getMessage(), e)
            );
    }

    /** 获取某个用户上架至市场的所有武器的 ID。*/
    @Override
    public Mono<ServerResponse>
    findAllWeaponIdsFromMarketByUUID(ServerRequest request)
    {
        return
        prasePathVariable(request, "userId")
            .flatMap((uuid) ->
                this.userRedisService
                    .getAllWeaponIdsFromMarketByUUID(uuid)
                    .collectList()
                    .flatMap((weaponIds) -> {
                        if (weaponIds.isEmpty())
                        {
                            return
                            this.responseBuilder
                                .NOT_FOUND(
                                    String.format("User: %s don't selling any weapons in market.", uuid),
                                    null
                                );
                        }

                        return
                        this.responseBuilder
                            .OK(
                                weaponIds,
                                String.format("Find %s weapons ID in market.", weaponIds.size()),
                                null, null
                            );
                    })
                    .onErrorResume((exception) ->
                        this.responseBuilder
                            .INTERNAL_SERVER_ERROR(
                                String.format(
                                    "Find weapon ID from market for user: %s failed!", uuid),
                                exception
                            )
                    )
            )
            .onErrorResume(
                IllegalArgumentException.class,
                (e) ->
                    this.responseBuilder
                        .BAD_REQUEST(e.getMessage(), e)
            );
    }

    /** 获取某个用户的数据。*/
    @Override
    public Mono<ServerResponse>
    findUserInfoByUUID(ServerRequest request)
    {
        return
        prasePathVariable(request, "userId")
            .flatMap((uuid) ->
                this.userRedisService
                    .getUserInfoByUUID(uuid)
                    .flatMap((userInfo) ->
                        this.responseBuilder.OK(
                                userInfo,
                                String.format("Find info with user %s", uuid),
                                null, null
                            ))
                    .onErrorResume(
                        NoSuchElementException.class,
                        (e) ->
                            this.responseBuilder
                                .NOT_FOUND(e.getMessage(), e))
                    .onErrorResume(
                        (e) ->
                            this.responseBuilder
                                .INTERNAL_SERVER_ERROR(
                                    String.format("Find info for user: %s failed!", uuid),
                                    null
                                )
                    )
            ).onErrorResume(
                IllegalArgumentException.class,
                (e) ->
                    this.responseBuilder
                        .BAD_REQUEST(e.getMessage(), e)
            );
    }

    /** 创建一个新用户，并为它随机挑选几件武器放入包裹。*/
    @Override
    public Mono<ServerResponse>
    createNewUser(ServerRequest request)
    {
        return
        praseRequestParam(request, "name")
            .flatMap((userName) ->
                this.userRedisService
                    .addNewUser(userName)
                    .flatMap((uuid) ->
                        this.responseBuilder
                            .OK(
                                null,
                                String.format(
                                    "Welcome new user: %s, your uuid: %s",
                                    userName, uuid
                                ), null, null
                            ))
                    .onErrorResume(
                        IllegalArgumentException.class,
                        (e) ->
                            this.responseBuilder
                                .BAD_REQUEST(e.getMessage(), e)
                    )
                    .onErrorResume((e) ->
                        this.responseBuilder
                            .INTERNAL_SERVER_ERROR("Create new user error!", e)
                    )
                )
            .onErrorResume(
                IllegalArgumentException.class,
                (e) ->
                    this.responseBuilder
                        .BAD_REQUEST(e.getMessage(), e)
            );
    }

    @Override
    public Mono<ServerResponse> 
    addNewContact(ServerRequest request) 
    {
        return
        Mono.zip(
            prasePathVariable(request, "userId"),
            praseRequestParam(request, "contactName"))
        .flatMap((params) -> {
            final String uuid        = params.getT1();
            final String contactName = params.getT2();

            return
            this.userRedisService
                .addNewContact(uuid, contactName)
                .then(
                    this.responseBuilder
                        .OK(
                            null, 
                            String.format("OK! User %s become your latest contact!", contactName),
                            null, null
                        ))
                .onErrorResume(
                    IllegalArgumentException.class, 
                        (exception) -> 
                            this.responseBuilder
                                .BAD_REQUEST(exception.getMessage(), exception)
                )
                .onErrorResume((exception) -> 
                    this.responseBuilder
                        .INTERNAL_SERVER_ERROR(
                            String.format(
                                "Add new contact <%s> for user %s failed!", 
                                contactName, uuid
                            ), exception
                        )
                );
        })
        .onErrorResume(
            IllegalArgumentException.class, 
            (exception) -> 
                this.responseBuilder
                    .BAD_REQUEST(exception.getMessage(), exception)
        );
    }

    @Override
    public Mono<ServerResponse> 
    removeContact(ServerRequest request) 
    {
        return
        Mono.zip(
            prasePathVariable(request, "userId"),
            prasePathVariable(request, "contactName"))
        .flatMap((params) -> {
            final String uuid        = params.getT1();
            final String contactName = params.getT2();

            return
            this.userRedisService
                .removeContact(uuid, contactName)
                .then(
                    this.responseBuilder.OK(
                        null,
                        String.format(
                            "Remove %s to user: %s's contact list!", 
                            contactName, uuid
                        ), null, null
                    ))
                .onErrorResume(
                    NoSuchElementException.class,
                        (exception) -> 
                            this.responseBuilder
                                .NOT_FOUND(exception.getMessage(), exception))
                .onErrorResume((exception) -> 
                    this.responseBuilder
                        .INTERNAL_SERVER_ERROR(
                            String.format(
                                "Remove contact <%s> for user %s failed!", 
                                contactName, uuid
                            ), exception
                        )
                );
        })
        .onErrorResume(
            IllegalArgumentException.class, 
            (exception) -> 
                this.responseBuilder
                    .BAD_REQUEST(exception.getMessage(), exception)
        );
    }

    @Override
    public Mono<ServerResponse> 
    addWeaponToInventory(ServerRequest request)
    {
        return
        Mono.zip(
            prasePathVariable(request, "userId"),
            praseRequestParam(request, "weaponName"))
        .flatMap((params) -> {
            final String  uuid   = params.getT1();
            final Weapons weapon = Weapons.valueOf(params.getT2());

            return
            this.userRedisService
                .addWeaponToInventory(uuid, weapon)
                .then(
                    this.responseBuilder.OK(
                        null, 
                        String.format(
                            "Add new weapon: <%s> to user: %s's inventory!",
                            weapon, uuid
                        ), null, null
                    ))
                .onErrorResume(
                    IllegalArgumentException.class,
                    (exception) ->
                        this.responseBuilder
                            .BAD_REQUEST(exception.getMessage(), exception)
                )
                .onErrorResume(
                    (exception) ->
                        this.responseBuilder
                            .INTERNAL_SERVER_ERROR(
                                String.format(
                                    "Add weapon %s from user %s inventory failed!",
                                    weapon, uuid
                                ),
                        exception
                    )
                );
        })
        .onErrorResume(
            IllegalArgumentException.class,
            (exception) ->
                this.responseBuilder
                    .BAD_REQUEST(exception.getMessage(), exception)
        );
    }

    @Override
    public Mono<ServerResponse> 
    destroyWeaponFromInventory(ServerRequest request)
    {
       return
       Mono.zip(
            prasePathVariable(request, "userId"),
            prasePathVariable(request, "weaponName"))
       .flatMap((params) -> {
            final String  uuid   = params.getT1();
            final Weapons weapon = Weapons.valueOf(params.getT2());

            return
            this.userRedisService
                .destroyWeaponFromInventory(uuid, weapon)
                .then(
                    this.responseBuilder.OK(
                        null, 
                        String.format("Already destory weapon: <%s> to your inventory!", weapon), 
                        null, null
                    ))
                .onErrorResume(
                    NoSuchElementException.class, 
                    (exception) ->
                        this.responseBuilder
                            .NOT_FOUND(exception.getMessage(), exception)
                )
                .onErrorResume(
                    (exception) -> 
                        this.responseBuilder
                            .INTERNAL_SERVER_ERROR(
                                String.format(
                                    "Destory weapon %s from user %s inventory failed!",
                                    weapon, uuid
                                ), 
                        exception
                    )
                );
       })
       .onErrorResume(
            IllegalArgumentException.class, 
            (exception) ->
                this.responseBuilder
                    .BAD_REQUEST(exception.getMessage(), exception)
       );
    }

    @Override
    public Mono<ServerResponse> 
    addWeaponToMarket(ServerRequest request)
    {
        return
        Mono.zip(
            prasePathVariable(request, "userId"),
            praseRequestParam(request, "weaponName").map(Weapons::valueOf),
            praseNumberRequestParam(request, "price", Double::parseDouble))
        .flatMap((params) -> {
            final String  uuid   = params.getT1();
            final Weapons weapon = params.getT2();
            final double  price  = params.getT3();

            return
            this.userRedisService
                .addWeaponToMarket(uuid, weapon, price)
                .flatMap((weaponId) ->
                    this.responseBuilder
                        .OK(
                            null,
                            String.format(
                                "Add weapon: %s to market success! " +
                                "Market Weapon ID: [%s]",
                                weapon, weaponId
                            ), 
                            null, null
                        ))
                .onErrorResume(
                    NoSuchElementException.class,
                    (exception) ->
                        this.responseBuilder
                            .NOT_FOUND(exception.getMessage(), exception)
                )
                .onErrorResume(
                    (exception) -> 
                        this.responseBuilder
                            .INTERNAL_SERVER_ERROR(
                                String.format(
                                    "Add weapon (which is: %s) for user: %s failed!",
                                    weapon, uuid
                                ),
                        exception
                    )
                );
        })
        .onErrorResume(
            IllegalArgumentException.class, 
            (exception) ->
                this.responseBuilder
                    .BAD_REQUEST(exception.getMessage(), exception)
        );
    }

    @Override
    public Mono<ServerResponse>
    removeWeaponFromMarket(ServerRequest request)
    {
        return
        Mono.zip(
            prasePathVariable(request, "userId"),
            prasePathVariable(request, "weaponName"))
        .flatMap((params) -> {
            final String  uuid   = params.getT1();
            final Weapons weapon = Weapons.valueOf(params.getT2());

            return
            this.userRedisService
                .removeWeaponFromMarket(uuid, weapon)
                .then(
                    this.responseBuilder
                        .OK(
                            null, 
                            String.format(
                                "Remove weapon %s from market, back to user: %s.",
                                weapon, uuid
                            ), null, null
                        ))
                .onErrorResume(
                    NoSuchElementException.class,
                    (exception) ->
                        this.responseBuilder.NOT_FOUND(
                            String.format(
                                "User %s unlist weapon: %s on market!",
                                uuid, weapon.getItemName()
                            ), exception
                        )
                )
                .onErrorResume((exception) ->
                    this.responseBuilder
                        .INTERNAL_SERVER_ERROR(
                            String.format(
                                "Remove user: %s 's weapon %s from market faild!",
                                uuid, weapon
                            ), exception
                        )
                );
        })
        .onErrorResume(
            IllegalArgumentException.class,
            (exception) ->
                this.responseBuilder
                    .BAD_REQUEST(exception.getMessage(), exception)
        );
    }

    @Override
    public Mono<ServerResponse> 
    deleteUser(ServerRequest request) 
    {
        return
        prasePathVariable(request, "userId")
            .flatMap((uuid) ->
                this.userRedisService
                    .deleteUser(uuid)
                    .then(
                        this.responseBuilder
                            .OK(
                                null,
                                String.format("Delete user: %s complete! Bye!", uuid),
                            null, null
                        ))
                    .onErrorResume(
                        NoSuchElementException.class,
                        (exception) ->
                            this.responseBuilder
                                .NOT_FOUND(exception.getMessage(), exception))
                    .onErrorResume(
                        IllegalArgumentException.class, 
                        (exception) ->
                            this.responseBuilder
                                .BAD_REQUEST(exception.getMessage(), exception))
                    .onErrorResume(
                        (exception) ->
                            this.responseBuilder
                                .INTERNAL_SERVER_ERROR(
                                    String.format("Faild to delete user: %s!", uuid), 
                                    exception)
                    )
            )
            .onErrorResume(
                IllegalArgumentException.class,
                (exception) ->
                    this.responseBuilder
                        .BAD_REQUEST(exception.getMessage(), exception)
            );
    }
}