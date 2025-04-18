package com.esprit.models.enums;

public enum GoalTypeEnum {
    NONE,
    EVENT_COUNT,
    EVENT_LIKES,
    MEMBER_COUNT;

    public static GoalTypeEnum fromString(String value) {
        if (value == null || value.isEmpty()) {
            return NONE;
        }
        
        return switch (value.toUpperCase()) {
            case "EVENTS_COUNT" -> EVENT_COUNT;
            case "EVENTS_LIKES" -> EVENT_LIKES;
            case "MEMBERS_COUNT" -> MEMBER_COUNT;
            case "NONE" -> NONE;
            default -> GoalTypeEnum.valueOf(value.toUpperCase());
        };
    }
}
