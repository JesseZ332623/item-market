package com.example.jesse.item_market.email_send_task.dto;

import lombok.Getter;

/** 邮件任务的优先级枚举。*/
public enum TaskPriority
{
    LOW("low", 30.00),
    MEIDUM("meidum", 60.00),
    HIGH("high", 90.00);

    @Getter
    final String priorityName;

    @Getter
    final double priorityScore;

    TaskPriority(String p, double s) {
        this.priorityName  = p;
        this.priorityScore = s;
    }
}
