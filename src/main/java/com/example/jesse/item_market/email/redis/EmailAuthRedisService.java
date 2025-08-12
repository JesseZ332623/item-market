package com.example.jesse.item_market.email.redis;

import com.example.jesse.item_market.email.service.EmailAuthQueryService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static com.example.jesse.item_market.email.authkey.EmailAuthRedisKey.ENTERPRISE_EMAIL_ADDRESS;
import static com.example.jesse.item_market.email.authkey.EmailAuthRedisKey.SERVICE_AUTH_CODE;
import static com.example.jesse.item_market.errorhandle.RedisErrorHandle.redisGenericErrorHandel;

/** 邮箱服务授权码服务类。*/
@Slf4j
@Service
public class EmailAuthRedisService
{
    @Autowired
    private EmailAuthQueryService emailAuthQueryService;

    @Autowired
    private ReactiveRedisTemplate<String, Object> redisTemplate;

    /**
     * 在所以依赖注入都完成后，
     * 将邮箱发送人的邮箱号和服务授权码读出，按指定 key 存入 Redis（自动执行）。
     */
    @PostConstruct
    public void readEmailPublisherInfo()
    {
        this.emailAuthQueryService
            .findEmailPublisherInfoById(1)
            .flatMap(
                (publisherInfo) ->
                    this.redisTemplate.opsForValue()
                        .set(ENTERPRISE_EMAIL_ADDRESS.toString(), publisherInfo.getEmail())
                        .then(this.redisTemplate.opsForValue()
                                 .set(SERVICE_AUTH_CODE.toString(), publisherInfo.getEmailAuthCode())
                        )
            )
            .timeout(Duration.ofSeconds(5L))
            .onErrorResume((exception) ->
                redisGenericErrorHandel(exception, null))
            .doOnSuccess((isSuccess) ->
                log.info(
                    "Read email publisher info to redis complete! Result: {}",
                    isSuccess
                )
            ).subscribe();
    }
}
