package com.yupi.yupaobackend.model.enums;

/**
 * 添加好友状态枚举类
 */
public enum AddFriendStatusEnum {

    ADDING(0, "添加中"),
    ADD_SUCCESS(1, "添加成功"),
    ADD_ERROR(2, "添加失败");

    private int value;

    private String text;

    public static AddFriendStatusEnum getEnumByValue(Integer value){
        if (value == null){
            return null;
        }
        AddFriendStatusEnum[] values = AddFriendStatusEnum.values();
        for (AddFriendStatusEnum teamStatusEnum : values) {
            if (teamStatusEnum.getValue() == value){
                return teamStatusEnum;
            }
        }
        return null;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    AddFriendStatusEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }
}
