package com.example.jesse.item_market.user;

import lombok.Getter;

/** 十八般兵器枚举类。*/
public enum Weapons
{
    Broadsword("Broadsword"),

    Spear("Spear"),

    Sword("Sword"),

    Halberd("Halberd"),

    Axe("Axe"),

    HookSword("HookSword"),

    Trident("Trident"),

    Whip("Whip"),

    Mace("Mace"),

    DaggerAxe("DaggerAxe"),

    TridentHalberd("TridentHalberd"),

    Staff("Staff"),

    LongLance("LongLance"),

    Tonfa("Tonfa"),

    MeteorHammer("MeteorHammer");

    @Getter
    final private String itemName;

    Weapons(String name) { this.itemName = name; }
}
