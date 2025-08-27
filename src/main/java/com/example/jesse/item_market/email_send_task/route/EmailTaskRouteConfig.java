package com.example.jesse.item_market.email_send_task.route;

import com.example.jesse.item_market.email_send_task.service.EmailTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static com.example.jesse.item_market.email_send_task.route.EmailTaskURI.*;

/** 邮件任务执行服务路由函数配置类。*/
@Slf4j
@Configuration
public class EmailTaskRouteConfig
{
    @Autowired
    private EmailTaskService emailTaskService;

    @Bean
    public RouterFunction<ServerResponse>
    emailTaskRouteFunction()
    {
        return
        RouterFunctions
            .route()
            .PUT(START_EMAIL_TASK_URI,       this.emailTaskService::startEmailTask)
            .PUT(STOP_EMAIL_TASK_URI,        this.emailTaskService::stopEmailTask)
            .PUT(STOP_POLL_DELAY_ZSET_URI,   this.emailTaskService::stopPollDelayZset)
            .PUT(STOP_EMAIL_SENDER_TASK_URI, this.emailTaskService::stopExecuteEmailSenderTask)
            .POST(ADD_EMAIL_TASK_URI,        this.emailTaskService::addEmailTask)
            .POST(ADD_VERIFY_CODE_EMAIL_TASK_URI, this.emailTaskService::addVerifyCodeEmailTask)
            .POST(ADD_TEXT_EMAIL_TASK_URI,       this.emailTaskService::addTextEmailTask)
            .POST(ADD_ATTACHMENT_EMAIL_TASK_URI, this.emailTaskService::addAttechmentEmailTask)
            .build();
    }
}