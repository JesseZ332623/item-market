package com.example.jesse.item_market.guild;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/** 公会 Redis 操作接口类。*/
public interface GuildRedisService
{
    /** 按公会名搜索所有公会成员的 UUID。*/
    Flux<String> findAllMembersByGuildName(String guildName);

    /**
     * 用户创建公会，并成为这个公会的 Leader。
     *
     * @param uuid      哪个用户要创建公会？
     * @param guildName 新公会的名字是？
     *
     * @return 不发布任何数据的 Mono，表示操作整体是否完成
     */
    Mono<Void> createGuild(String uuid, String guildName);

    /**
     * 用户加入公会。
     *
     * @param uuid      哪个用户要加入？
     * @param guildName 加入哪个公会？
     *
     * @return 不发布任何数据的 Mono，表示操作整体是否完成
     */
    Mono<Void> joinGuild(String uuid, String guildName);

    /**
     * 用户离开公会。
     *
     * @param uuid      哪个用户要离开？
     * @param guildName 离开哪个公会？
     *
     * @return 不发布任何数据的 Mono，表示操作整体是否完成
     */
    Mono<Void> leaveGuild(String uuid, String guildName);

    /**
     * 在搜索公会成员时，
     * 对于搜索框输入的 prefix，自动地补全全部匹配的成员名。
     *
     * @param guildName 搜索哪个公会的成员？
     * @param prefix    搜索框输入的内容
     *
     * @return 发布布公会成员中所有以 prefix 开头的成员名的 Flux
     */
    Mono<List<String>> fetchAutoCompleteMember(String guildName, String prefix);

    /**
     * 公会成员之间互发消息（可能用到 Redis 的 PUB 和 SUB），后续再研究。
     *
     * @param guildName 哪个公会？
     * @param sender    发送人
     * @param receiver  接收人
     * @param message   消息
     *
     * @return 不发布任何数据的 Mono，表示操作整体是否完成
     */
    Mono<Void> sendMessageBetweenMembers(
        String guildName,
        String sender, String receiver,
        String message
    );

    /**
     * Leader 解散公会，并向所有公会成员发送解散的消息（发送消息的功能后续再研究）。
     *
     * @param uuid      公会创始人 ID
     * @param guildName 公会名
     *
     * @return 不发布任何数据的 Mono，表示操作整体是否完成
     */
    Mono<Void> deleteGuild(String uuid, String guildName);
}
