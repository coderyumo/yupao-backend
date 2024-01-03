package com.yupi.yupaobackend.model.enums;

/**
 * 添加好友状态枚举类
 */
public enum AddFriendStatusEnum {
    PUBLIC(0, "公共"),
    PRIVATE(1, "私有"),
    SECRET(2, "加密");

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
