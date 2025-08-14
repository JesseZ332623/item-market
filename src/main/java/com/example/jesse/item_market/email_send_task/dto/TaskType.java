package com.example.jesse.item_market.email_send_task.dto;

import lombok.Getter;

/** 邮件任务类型枚举。*/
public enum TaskType
{
    /** 需要延迟执行的 */
    DELAY_TASK("delay-task"),

    /** 不需要延迟执行，但任务是有优先级的 */
    PRIORITY_TASK("priority-task"),

    /** 死信队列。*/
    DEATH_TASK("death-task");

    @Getter
    final String type;

    TaskType(String t) { this.type = t; }
}
