package com.example.jesse.item_market.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

/** users:log Stream DTO */
@Data
@ToString
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode
public class UserLogDTO
{
    private String recordId;
    private String event;
    private String uuid;
    private String userName;
    private String userFunds;
    private String timeStamp;
}
