package com.esprit.services;

import com.esprit.models.Commande;
import com.esprit.models.Orderdetails;
import com.esprit.models.Produit;
import com.esprit.models.User;
import com.esprit.models.enums.StatutCommandeEnum;
import com.esprit.utils.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommandeService {

    private Connection connection;

    public CommandeService() {
        this.connection = DataSource.getInstance().getCnx();
        populateSampleData(); // Automatically populate data on initialization
    }

    private void populateSampleData() {
        try {
            // Log existing users
            Statement checkUserStmt = connection.createStatement();
            ResultSet userRs = checkUserStmt.executeQuery("SELECT id, nom, prenom FROM user");
            System.out.println("Existing users:");
            while (userRs.next()) {
                System.out.println("User record: id=" + userRs.getInt("id") +
                        ", nom=" + userRs.getString("nom") +
                        ", prenom=" + userRs.getString("prenom"));
            }

            // Log existing products
            Statement checkProduitStmt = connection.createStatement();
            ResultSet produitRs = checkProduitStmt.executeQuery("SELECT * FROM produit");
            System.out.println("Existing products:");
            while (produitRs.next()) {
                System.out.println("Produit record: id=" + produitRs.getInt("id") +
                        ", nom_prod=" + produitRs.getString("nom_prod"));
            }

            // Log existing commandes
            Statement checkCommandeStmt = connection.createStatement();
            ResultSet commandeRs = checkCommandeStmt.executeQuery("SELECT * FROM commande");
            System.out.println("Existing commandes:");
            while (commandeRs.next()) {
                System.out.println("Commande record: id=" + commandeRs.getInt("id") +
                        ", user_id=" + commandeRs.getInt("user_id"));
            }

            // Log existing orderdetails
            Statement checkOrderDetailsStmt = connection.createStatement();
            ResultSet orderDetailsRs = checkOrderDetailsStmt.executeQuery("SELECT * FROM orderdetails");
            System.out.println("Existing orderdetails:");
            while (orderDetailsRs.next()) {
                System.out.println("Orderdetails record: id=" + orderDetailsRs.getInt("id") +
                        ", commande_id=" + orderDetailsRs.getInt("commande_id") +
                        ", produit_id=" + orderDetailsRs.getInt("produit_id") +
                        ", quantity=" + orderDetailsRs.getInt("quantity") +
                        ", price=" + orderDetailsRs.getDouble("price") +
                        ", total=" + orderDetailsRs.getDouble("total"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error checking existing data: " + e.getMessage());
        }
    }

    public void createCommande(Commande commande) {
        try {
            // Vérifier que l'utilisateur est défini
            if (commande.getUser() == null || commande.getUser().getId() <= 0) {
                throw new SQLException("L'utilisateur doit être défini pour créer une commande");
            }

            String insertCommandeSQL = "INSERT INTO commande (date_comm, statut, user_id) VALUES (?, ?, ?)";
            PreparedStatement ps = connection.prepareStatement(insertCommandeSQL, Statement.RETURN_GENERATED_KEYS);
            ps.setDate(1, Date.valueOf(commande.getDateComm()));
            ps.setString(2, commande.getStatut().name());
            ps.setInt(3, commande.getUser().getId());
            
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La création de la commande a échoué, aucune ligne affectée.");
            }

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int commandeId = rs.getInt(1);
                commande.setId(commandeId);

                // Insérer les détails de la commande si présents
                if (commande.getOrderDetails() != null && !commande.getOrderDetails().isEmpty()) {
                    for (Orderdetails detail : commande.getOrderDetails()) {
                        String insertDetailSQL = "INSERT INTO orderdetails (commande_id, produit_id, quantity, price, total) VALUES (?, ?, ?, ?, ?)";
                        PreparedStatement detailPs = connection.prepareStatement(insertDetailSQL);
                        detailPs.setInt(1, commandeId);
                        detailPs.setInt(2, detail.getProduit().getId());
                        detailPs.setInt(3, detail.getQuantity());
                        detailPs.setDouble(4, detail.getPrice());
                        detailPs.setDouble(5, detail.getTotal());
                        detailPs.executeUpdate();
                    }
                }
            }

            System.out.println("Commande créée avec succès pour l'utilisateur ID: " + commande.getUser().getId());
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la création de la commande: " + e.getMessage());
        }
    }

    public void supprimerCommande(int id) {
        try {
            connection.setAutoCommit(false);

            try {
                String deleteDetailsSQL = "DELETE FROM orderdetails WHERE commande_id = ?";
                PreparedStatement ps1 = connection.prepareStatement(deleteDetailsSQL);
                ps1.setInt(1, id);
                int detailsDeleted = ps1.executeUpdate();
                System.out.println("Order details deleted: " + detailsDeleted);

                String deleteCommandeSQL = "DELETE FROM commande WHERE id = ?";
                PreparedStatement ps2 = connection.prepareStatement(deleteCommandeSQL);
                ps2.setInt(1, id);
                int commandesDeleted = ps2.executeUpdate();
                System.out.println("Commands deleted: " + commandesDeleted);

                if (commandesDeleted > 0) {
                    connection.commit();
                    System.out.println("Commande supprimée avec succès.");
                } else {
                    connection.rollback();
                    System.out.println("Commande avec ID " + id + " non trouvée.");
                }
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Erreur lors de la suppression de la commande: " + e.getMessage());
        }
    }

    public void validerCommande(int id) {
        try {
            String updateSQL = "UPDATE commande SET statut = ? WHERE id = ?";
            PreparedStatement ps = connection.prepareStatement(updateSQL);
            ps.setString(1, StatutCommandeEnum.CONFIRMEE.name());
            ps.setInt(2, id);
            ps.executeUpdate();

            System.out.println("Commande validée.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Commande> getAllCommandes(String keyword) {
        List<Commande> commandes = new ArrayList<>();
        try {
            String query = "SELECT c.*, u.id as user_id, u.nom as user_nom, u.prenom as user_prenom, u.email as user_email " +
                    "FROM commande c " +
                    "LEFT JOIN user u ON c.user_id = u.id";
            if (keyword != null && !keyword.isEmpty()) {
                query += " WHERE c.statut LIKE ?";
            }
            query += " ORDER BY c.date_comm DESC";

            PreparedStatement ps = connection.prepareStatement(query);
            if (keyword != null && !keyword.isEmpty()) {
                ps.setString(1, "%" + keyword + "%");
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Commande commande = new Commande();
                commande.setId(rs.getInt("id"));
                commande.setDateComm(rs.getDate("date_comm").toLocalDate());
                commande.setStatut(StatutCommandeEnum.valueOf(rs.getString("statut")));

                // Set the user
                int userId = rs.getInt("user_id");
                String userNom = rs.getString("user_nom");
                String userPrenom = rs.getString("user_prenom");
                String userEmail = rs.getString("user_email");

                // Only create a User object if the user exists (i.e., user_id is not NULL)
                if (!rs.wasNull()) { // Check if user_id was NULL
                    User user = new User();
                    user.setId(userId);
                    user.setFirstName(userNom);
                    user.setLastName(userPrenom);
                    user.setEmail(userEmail);
                    System.out.println("Loaded user for commande ID " + commande.getId() + ": " + user);
                    commande.setUser(user);
                } else {
                    System.err.println("Warning: No user found for commande ID " + commande.getId() + " with user_id " + userId);
                    commande.setUser(null);
                }

                // Fetch order details for this commande
                List<Orderdetails> orderDetails = new ArrayList<>();
                String detailsQuery = "SELECT od.*, p.nom_prod " +
                        "FROM orderdetails od " +
                        "JOIN produit p ON od.produit_id = p.id " +
                        "WHERE od.commande_id = ?";
                PreparedStatement detailsPs = connection.prepareStatement(detailsQuery);
                detailsPs.setInt(1, commande.getId());
                ResultSet detailsRs = detailsPs.executeQuery();

                double total = 0.0;
                while (detailsRs.next()) {
                    Orderdetails detail = new Orderdetails();
                    detail.setId(detailsRs.getInt("id"));
                    detail.setQuantity(detailsRs.getInt("quantity"));

                    // Use double directly, no casting needed
                    detail.setPrice(detailsRs.getDouble("price"));
                    detail.setTotal(detailsRs.getDouble("total"));

                    Produit produit = new Produit(detailsRs.getInt("produit_id"), detailsRs.getString("nom_prod"));
                    detail.setProduit(produit);

                    detail.setCommande(commande);

                    orderDetails.add(detail);
                    total += detail.getTotal();
                }
                commande.setTotal(total); // Set the total for the commande
                commande.setOrderDetails(orderDetails);

                commandes.add(commande);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Erreur lors de la récupération des commandes: " + e.getMessage());
        }
        return commandes;
    }

    public List<Object[]> getTopProduits() {
        List<Object[]> stats = new ArrayList<>();
        try {
            String sql = "SELECT p.nom_prod, SUM(od.quantity) as total_ventes " +
                    "FROM produit p " +
                    "JOIN orderdetails od ON p.id = od.produit_id " +
                    "GROUP BY p.id " +
                    "ORDER BY total_ventes DESC";
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                String nomProd = rs.getString("nom_prod");
                int totalVentes = rs.getInt("total_ventes");
                System.out.println("Product: " + nomProd + ", Sales: " + totalVentes);
                stats.add(new Object[] { nomProd, totalVentes });
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }
    
    /**
     * Get the current cart (commande with EN_COURS status) for a user
     * @param userId User ID
     * @return Cart commande or null if no cart exists
     */
    public Commande getCartForUser(int userId) {
        try {
            String query = "SELECT c.* FROM commande c WHERE c.user_id = ? AND c.statut = ? ORDER BY c.id DESC LIMIT 1";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, userId);
            ps.setString(2, StatutCommandeEnum.EN_COURS.name());
            
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Commande cart = new Commande();
                cart.setId(rs.getInt("id"));
                cart.setDateComm(rs.getDate("date_comm").toLocalDate());
                cart.setStatut(StatutCommandeEnum.EN_COURS);
                
                // Set the user
                User user = new User();
                user.setId(userId);
                cart.setUser(user);
                
                // Fetch cart items
                List<Orderdetails> orderDetails = new ArrayList<>();
                String detailsQuery = "SELECT od.*, p.* FROM orderdetails od " +
                                      "JOIN produit p ON od.produit_id = p.id " +
                                      "WHERE od.commande_id = ?";
                PreparedStatement detailsPs = connection.prepareStatement(detailsQuery);
                detailsPs.setInt(1, cart.getId());
                ResultSet detailsRs = detailsPs.executeQuery();
                
                double total = 0.0;
                while (detailsRs.next()) {
                    Orderdetails detail = new Orderdetails();
                    detail.setId(detailsRs.getInt("id"));
                    detail.setQuantity(detailsRs.getInt("quantity"));
                    detail.setPrice(detailsRs.getDouble("price"));
                    detail.setTotal(detailsRs.getDouble("total"));
                    
                    Produit produit = new Produit();
                    produit.setId(detailsRs.getInt("produit_id"));
                    produit.setNomProd(detailsRs.getString("nom_prod"));
                    produit.setDescProd(detailsRs.getString("desc_prod"));
                    produit.setPrix(detailsRs.getFloat("prix"));
                    produit.setQuantity(String.valueOf(detailsRs.getInt("quantity")));
                    produit.setImgProd(detailsRs.getString("img_prod"));
                    
                    detail.setProduit(produit);
                    detail.setCommande(cart);
                    
                    orderDetails.add(detail);
                    total += detail.getTotal();
                }
                cart.setOrderDetails(orderDetails);
                cart.setTotal(total);
                
                return cart;
            }
            
            return null; // No cart found
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Update an existing order with new details
     * @param commande The commande to update
     */
    public void updateCommande(Commande commande) {
        try {
            connection.setAutoCommit(false);
            
            try {
                // Update the commande
                String updateCommandeSQL = "UPDATE commande SET date_comm = ?, statut = ? WHERE id = ?";
                PreparedStatement ps = connection.prepareStatement(updateCommandeSQL);
                ps.setDate(1, Date.valueOf(commande.getDateComm()));
                ps.setString(2, commande.getStatut().name());
                ps.setInt(3, commande.getId());
                ps.executeUpdate();
                
                // Delete existing order details
                String deleteDetailsSQL = "DELETE FROM orderdetails WHERE commande_id = ?";
                PreparedStatement deletePs = connection.prepareStatement(deleteDetailsSQL);
                deletePs.setInt(1, commande.getId());
                deletePs.executeUpdate();
                
                // Insert new order details
                for (Orderdetails detail : commande.getOrderDetails()) {
                    String insertDetailSQL = "INSERT INTO orderdetails (commande_id, produit_id, quantity, price, total) VALUES (?, ?, ?, ?, ?)";
                    PreparedStatement detailPs = connection.prepareStatement(insertDetailSQL);
                    detailPs.setInt(1, commande.getId());
                    detailPs.setInt(2, detail.getProduit().getId());
                    detailPs.setInt(3, detail.getQuantity());
                    detailPs.setDouble(4, detail.getPrice());
                    detailPs.setDouble(5, detail.getTotal());
                    detailPs.executeUpdate();
                }
                
                connection.commit();
                System.out.println("Commande updated successfully: ID " + commande.getId());
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error updating commande: " + e.getMessage());
        }
    }
}
