package com.example.jesse.item_market.email_send_task.dto;

import lombok.Getter;

/** 邮件任务的优先级枚举。*/
public enum TaskPriority
{
    LOW("low", 25.00),
    MEDIUM_LOW("medium_low", 50.00),
    MEDIUM("medium", 60.00),
    MEDIUM_HIGH("medium_high", 70.00),
    HIGH("high", 80.00),
    VERY_HIGH("very_high", 90.00),
    CRITICAL("critical", 100.00);

    @Getter
    final String priorityName;

    @Getter
    final double priorityScore;

    TaskPriority(String p, double s) {
        this.priorityName  = p;
        this.priorityScore = s;
    }
}
