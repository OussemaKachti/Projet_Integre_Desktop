package com.esprit.utils;

import com.esprit.models.User;

public class SessionManager {
    
    private static SessionManager instance;
    private User currentUser;
    
    private SessionManager() {}
    
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
    
    public User getCurrentUser() {
        return currentUser;
    }
    
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    
    public boolean hasRole(String role) {
        if (currentUser == null) {
            return false;
        }
        return currentUser.getRole().name().equals(role);
    }
    
    public void logout() {
        currentUser = null;
    }
    public void clearSession() {
    this.currentUser = null;
    // If you have any other session data, clear it here
}
}
