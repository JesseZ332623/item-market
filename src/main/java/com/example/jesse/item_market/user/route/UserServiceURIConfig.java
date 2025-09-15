package com.example.jesse.item_market.user.route;

/** 用户服务路径配置类。 */
public class UserServiceURIConfig
{
    /** 用户服务根 URI */
    private final static String
    USER_ROOT_URI = "/api/user";

    /** (GET) 获取所有用户的 UUID */
    public final static String
    FIND_ALL_USER_UUID_URI = USER_ROOT_URI + "/find-all-user-uuid";

    /** (GET) 获取某个用户的最近联系人列表 */
    public final static String
    FIND_CONTACTS_BY_UUID_URI = USER_ROOT_URI + "/find-contacts";

    /** (GET) 获取某个用户包裹中的所有武器 */
    public final static String
    FIND_ALL_WEAPONS_FROM_INVENTORY_URI 
        = USER_ROOT_URI + "/find-weapons-from-inventory";

    /** (GET) 获取某个用户上架至市场的所有武器 */
    public final static String
    FIND_ALL_WEAPONS_FROM_MARKET_URI 
        = USER_ROOT_URI + "/find-weapons-from-market";

    /** (GET) 获取某个用户上架至市场的所有武器的 ID */
    public final static String
    FIND_ALL_WEAPONIDS_FROM_MARKET_URI 
        = USER_ROOT_URI + "/find-weaponIds-from-market";
    
    /** (GET) 获取某个用户的数据。*/
    public final static String
    FIND_USER_INFO_URI = USER_ROOT_URI + "/find-user-info";

    /** (POST) 创建一个新用户，并为它随机挑选几件武器放入包裹 */
    public final static String
    CREATE_NEW_USER_URI = USER_ROOT_URI + "/create-user";

    /** (POST) 用户记录另一个用户为最近联系人 */
    public final static String
    ADD_NEW_CONTACT_URI = USER_ROOT_URI + "/add-new-contact";

    /** (DELETE) 用户删除自己最近联系人列表的某个用户 */
    public final static String
    REMOVE_CONTACT_URI = USER_ROOT_URI + "/remove-contact";

    /** (POST) 为用户的包裹添加一件武器 */
    public final static String
    ADD_WEAPON_TO_INVENTORY_URI 
        = USER_ROOT_URI + "/add-weapon-to-inventory";

    /** (DELETE) 用户销毁包裹中的某个武器 */
    public final static String
    DESTORY_WEAPON_FROM_INVENTORY
        = USER_ROOT_URI + "/destory-weapon-from-inventory";

    /** (POST) 用户将武器上架至市场 */
    public final static String
    ADD_WEAPON_TO_MARKET_URI 
        = USER_ROOT_URI + "/add-weapon-to-market";

    /** (DELETE) 用户从市场上下架某个武器 */
    public final static String
    REMOVE_WEAPON_FROM_MARKET
        = USER_ROOT_URI + "/remove-weapon-from-market";

    /** (DELETE) 删除用户 */
    public final static String
    DELETE_USER = USER_ROOT_URI + "/delete-user";
}