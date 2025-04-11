package com.esprit.models.enums;

public enum StatutCommandeEnum {
    EN_COURS("en_cours"),
    CONFIRMEE("confirmee"),
    ANNULEE("annulee");

    private final String value;

    // Constructeur de l'énumération
    StatutCommandeEnum(String value) {
        this.value = value;
    }

    // Getter pour la valeur
    public String getValue() {
        return value;
    }

    // Méthode statique pour obtenir tous les valeurs de l'énumération
    public static String[] getValues() {
        return new String[] {
                EN_COURS.getValue(),
                CONFIRMEE.getValue(),
                ANNULEE.getValue()
        };
    }
}

