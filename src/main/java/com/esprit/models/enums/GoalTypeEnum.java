package com.esprit.models.enums;

public enum GoalTypeEnum {
    EVENT_COUNT,
    EVENT_LIKES,
    MEMBER_COUNT;

    public static GoalTypeEnum fromString(String value) {
        if (value == null || value.isEmpty()) {
            // Default to EVENT_COUNT instead of NONE
            return EVENT_COUNT;
        }

        return switch (value.toUpperCase()) {
            case "EVENTS_COUNT", "EVENT_COUNT" -> EVENT_COUNT;
            case "EVENTS_LIKES", "EVENT_LIKES" -> EVENT_LIKES;
            case "MEMBERS_COUNT", "MEMBER_COUNT" -> MEMBER_COUNT;
            default -> {
                try {
                    yield GoalTypeEnum.valueOf(value.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // If the value doesn't match any enum, default to EVENT_COUNT
                    yield EVENT_COUNT;
                }
            }
        };
    }
}