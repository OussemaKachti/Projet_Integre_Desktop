package com.esprit.services;

import com.esprit.models.User;
import com.esprit.models.enums.RoleEnum;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.esprit.utils.DataSource; // Ajoutez cet import

public class UserService {
    private Connection conn;
    private static UserService instance;

    public UserService() {
        conn = DataSource.getInstance().getCnx();
    }

    public static UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    public void add(User user) throws SQLException {
        String query = "INSERT INTO user (nom, prenom, email, password, tel, role, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, user.getNom());
            pst.setString(2, user.getPrenom());
            pst.setString(3, user.getEmail());
            pst.setString(4, user.getPassword()); // Consider hashing
            pst.setString(5, user.getTel());
            pst.setString(6, user.getRole().toString());
            // pst.setString(7, user.getStatus());

            pst.executeUpdate();

            try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public User getById(int id) throws SQLException {
        String query = "SELECT * FROM user WHERE id = ?";
        try (PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    public List<User> getAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM user";
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }

    public void update(User user) throws SQLException {
        String query = "UPDATE user SET nom=?, prenom=?, email=?, tel=?, role=?, status=? WHERE id=?";
        try (PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, user.getNom());
            pst.setString(2, user.getPrenom());
            pst.setString(3, user.getEmail());
            pst.setString(4, user.getTel());
            pst.setString(5, user.getRole().toString());
            // pst.setString(6, user.getStatus());
            pst.setInt(7, user.getId());

            pst.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String query = "DELETE FROM user WHERE id=?";
        try (PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setEmail(rs.getString("email"));

        // Le champ tel peut ne pas exister dans la base Symfony
        try {
            user.setTel(rs.getString("tel"));
        } catch (SQLException e) {
            // Ignorer si la colonne n'existe pas
        }

        // Gestion adaptative du rôle
        String roleStr = rs.getString("role");
        try {
            // 1. Essayer la valeur directe
            user.setRole(RoleEnum.valueOf(roleStr));
        } catch (IllegalArgumentException e) {
            try {
                // 2. Essayer la valeur en majuscules
                user.setRole(RoleEnum.valueOf(roleStr.toUpperCase()));
            } catch (IllegalArgumentException ex) {
                try {
                    // 3. Traiter les cas spécifiques connus
                    if (roleStr.equalsIgnoreCase("presidentClub") ||
                            roleStr.equalsIgnoreCase("PRESIDENTCLUB")) {
                        user.setRole(RoleEnum.PRESIDENT_CLUB);
                    } else if (roleStr.equalsIgnoreCase("administrateur") ||
                            roleStr.equalsIgnoreCase("admin")) {
                        user.setRole(RoleEnum.ADMINISTRATEUR);
                    } else if (roleStr.equalsIgnoreCase("membre") ||
                            roleStr.equalsIgnoreCase("user")) {
                        user.setRole(RoleEnum.MEMBRE);
                    } else {
                        // 4. Valeur par défaut
                        System.out.println("Rôle inconnu: " + roleStr + ", utilisation de MEMBRE par défaut");
                        user.setRole(RoleEnum.MEMBRE);
                    }
                } catch (Exception exc) {
                    // En cas d'erreur, utiliser MEMBRE par défaut
                    user.setRole(RoleEnum.MEMBRE);
                }
            }
        }

        return user;
    }

    public User authenticate(String email, String password) throws SQLException {
        String query = "SELECT * FROM user WHERE email = ? AND password = ?";
        try (PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, email);
            pst.setString(2, password); // Consider using hashing

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }
}