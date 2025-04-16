package com.esprit.models.enums;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum RoleEnum {
    NON_MEMBRE,
    MEMBRE,
    PRESIDENT_CLUB,
    ADMINISTRATEUR;

    public static List<String> getValues() {
        return Arrays.stream(values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    public static boolean isValid(String role) {
        try {
            RoleEnum.valueOf(role);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static RoleEnum fromString(String role) {
        try {
            return RoleEnum.valueOf(role);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}