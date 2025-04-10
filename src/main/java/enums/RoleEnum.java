
package enums;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum RoleEnum {
    NON_MEMBRE("nonMembre"),
    MEMBRE("membre"),
    PRESIDENT_CLUB("presidentClub"),
    ADMINISTRATEUR("administrateur");
    

    private final String value;

    RoleEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static List<String> getValues() {
        return Arrays.stream(values())
                .map(RoleEnum::getValue)
                .collect(Collectors.toList());
    }

    public static boolean isValid(String role) {
        return getValues().contains(role);
    }

    public static RoleEnum fromString(String role) {
        return Arrays.stream(values())
                .filter(e -> e.value.equals(role))
                .findFirst()
                .orElse(null);
    }
}