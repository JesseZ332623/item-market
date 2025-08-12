package com.example.jesse.item_market.email.service;

import com.example.jesse.item_market.email.entity.EmailAuthTable;
import reactor.core.publisher.Mono;

/** 邮箱服务授权信息服务接口类。*/
public interface EmailAuthQueryService
{
    /**
     * 通过邮箱号查询这个邮箱的服务授权码。
     *
     * @param email 邮箱号
     *
     * @return 授权码字符串
     */
    Mono<String>
    findAuthCodeByEmail(String email);

    /**
     * 通过 ID 查询整个邮箱发布者信息。
     *
     * <p>Tips: 这只是一个安全措施，id 是明确的。</p>
     */
    Mono<EmailAuthTable>
    findEmailPublisherInfoById(Integer id);
}
