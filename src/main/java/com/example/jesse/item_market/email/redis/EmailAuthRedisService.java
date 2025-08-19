package com.example.jesse.item_market.email.redis;

import com.example.jesse.item_market.email.service.EmailAuthQueryService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static com.example.jesse.item_market.email.authkey.EmailAuthRedisKey.ENTERPRISE_EMAIL_ADDRESS;
import static com.example.jesse.item_market.email.authkey.EmailAuthRedisKey.SERVICE_AUTH_CODE;
import static com.example.jesse.item_market.errorhandle.ProjectErrorHandle.projectGenericErrorHandel;

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
    public void init() {
        // 初始执行一次
        this.readEmailPublisherInfo();
    }

    /**
     * 后续开启定时任务，
     * 每 1 秒执行一次，确保发送人信息，授权码一直都在。
     */
    @Scheduled(fixedRate = 1000)
    public void readEmailPublisherInfo()
    {
        Mono.zip(
            this.redisTemplate.hasKey(ENTERPRISE_EMAIL_ADDRESS.toString()),
            this.redisTemplate.hasKey(SERVICE_AUTH_CODE.toString()))
        .flatMap((existRes) -> {

            // 如果这两个键已经存在了，就不要重复写入。
            if ((existRes.getT1() && existRes.getT2())) {
                return Mono.empty();
            }

            log.info("Flush email publish info.");

            return
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
                    projectGenericErrorHandel(exception, null));
        }).subscribe();
    }
}