package com.example.jesse.item_market.email.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** 邮箱服务授权码数据表实体。*/
@Data
@Table(name = "email_auth_table")
@NoArgsConstructor
@AllArgsConstructor
public class EmailAuthTable
{
    @Id
    @Column(value = "id")
    private Integer id;

    @Column(value = "email")
    private String  email;

    @Column(value = "email_auth_code")
    private String  emailAuthCode;
}
