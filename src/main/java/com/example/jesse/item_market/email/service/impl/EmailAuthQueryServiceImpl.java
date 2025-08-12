package com.example.jesse.item_market.email.service.impl;

import com.example.jesse.item_market.email.entity.EmailAuthTable;
import com.example.jesse.item_market.email.repository.EmailAuthRepository;
import com.example.jesse.item_market.email.service.EmailAuthQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** 邮箱服务授权信息服务实现类。*/
@Slf4j
@Component
public class EmailAuthQueryServiceImpl implements EmailAuthQueryService
{
    @Autowired
    private EmailAuthRepository emailAuthRepository;

    @Override
    public Mono<String> findAuthCodeByEmail(String email) {
        return this.emailAuthRepository.findAuthCodeByEmail(email);
    }

    @Override
    public Mono<EmailAuthTable>
    findEmailPublisherInfoById(Integer id) {
        return this.emailAuthRepository.findById(id);
    }
}
