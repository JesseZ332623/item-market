package com.example.jesse.item_market.utils;

import lombok.Getter;

/** 要读取的 Lua 脚本的操作类型枚举。*/
public enum LuaScriptOperatorType
{
    USER_OPERATOR("user-operator"),
    MARKET_OPERATOR("market-operator");

    @Getter
    final String typeName;

    LuaScriptOperatorType(String tpn) {
        this.typeName = tpn;
    }
}
