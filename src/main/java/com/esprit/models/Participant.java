package com.esprit.models;

import javafx.beans.property.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

    public class Participant {
        private final IntegerProperty id = new SimpleIntegerProperty();
        private final ObjectProperty<User> user = new SimpleObjectProperty<>();
        private final ObjectProperty<Club> club = new SimpleObjectProperty<>();
        private final StringProperty description = new SimpleStringProperty();
        private final StringProperty status = new SimpleStringProperty("en_attente"); // valeur par défaut

        // Constructeurs
        public Participant() {}

        public Participant(User user, Club club, String description) {
            this.user.set(user);
            this.club.set(club); 
            this.description.set(description);
            this.status.set("en_attente");
        }

        // Getters et Setters
        public int getId() {
            return id.get();
        }

        public IntegerProperty idProperty() {
            return id;
        }

        public void setId(int id) {
            this.id.set(id);
        }

        public User getUser() {
            return user.get();
        }

        public ObjectProperty<User> userProperty() {
            return user;
        }

        public void setUser(User user) {
            this.user.set(user);
        }

        public Club getClub() {
            return club.get();
        }

        public ObjectProperty<Club> clubProperty() {
            return club;
        }

        public void setClub(Club club) {
            this.club.set(club);
        }

        public String getDescription() {
            return description.get();
        }

        public StringProperty descriptionProperty() {
            return description;
        }

        public void setDescription(String description) {
            if (description == null || description.trim().isEmpty()) {
                throw new IllegalArgumentException("La description ne peut pas être vide.");
            }
            if (description.length() > 500) {
                throw new IllegalArgumentException("La description est trop longue.");
            }
            this.description.set(description);
        }


        public String getStatus() {
            return status.get();
        }

        public StringProperty statusProperty() {
            return status;
        }

        public void setStatus(String status) {
            this.status.set(status);
        }
    }



