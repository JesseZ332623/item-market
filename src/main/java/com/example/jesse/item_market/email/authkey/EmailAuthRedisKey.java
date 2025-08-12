package com.example.jesse.item_market.email.authkey;

import org.jetbrains.annotations.NotNull;

/** 项目中需要用到的所有 Redis 键枚举类。*/
public enum EmailAuthRedisKey implements CharSequence
{
    /**
     * <p>验证码发起者邮箱 Redis 键。</p>
     * <p>
     *     格式为：
     *     <pre>
     *         K: ENTERPRISE_EMAIL_ADDRESS
     *         V: String
     *     </pre>
     * </p>
     */
    ENTERPRISE_EMAIL_ADDRESS("ENTERPRISE_EMAIL_ADDRESS"),

    /**
     * <p>来自邮箱服务提供的授权码键。</p>
     * <p>
     *     格式为：
     *     <pre>
     *         K: SERVICE_AUTH_CODE
     *         V: String
     *     </pre>
     * </p>
     */
    SERVICE_AUTH_CODE("SERVICE_AUTH_CODE");

    final String keyName;

    EmailAuthRedisKey(String name) { this.keyName = name; }

    @Override
    public int length() { return this.keyName.length(); }

    @Override
    public char charAt(int index) {
        return this.keyName.charAt(index);
    }

    @Override
    public @NotNull CharSequence
    subSequence(int start, int end) {
        return this.keyName.subSequence(start, end);
    }

    @Override
    public @NotNull String toString() {
        return this.keyName;
    }
}