package com.esprit.utils;

import com.esprit.models.User;
import com.esprit.models.Club;

public class SessionManager {
    private static User currentUser;
    private static Club currentClub;

    static {
        // Créer un utilisateur statique pour le développement
        currentUser = new User();
        currentUser.setId(5);
        currentUser.setNom("Test");
        currentUser.setPrenom("User");
        currentUser.setEmail("test@test.com");

        // Optionnel : Créer un club statique si nécessaire
        currentClub = new Club();
        currentClub.setId(1);
        currentClub.setNom("Club Test");
        // currentClub.setPresident(currentUser); // Si besoin de tester en tant que
        // président
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static Club getCurrentClub() {
        return currentClub;
    }
}