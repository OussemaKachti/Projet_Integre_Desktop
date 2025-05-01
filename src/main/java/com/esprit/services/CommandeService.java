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
            String insertCommandeSQL = "INSERT INTO commande (date_comm, statut, user_id) VALUES (?, ?, ?)";
            PreparedStatement ps = connection.prepareStatement(insertCommandeSQL, Statement.RETURN_GENERATED_KEYS);
            ps.setDate(1, Date.valueOf(commande.getDateComm()));
            ps.setString(2, commande.getStatut().name());
            ps.setInt(3, commande.getUser().getId());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int commandeId = rs.getInt(1);

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

            System.out.println("Commande créée avec succès.");
        } catch (SQLException e) {
            e.printStackTrace();
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
            String query = "SELECT c.*, u.id as user_id, u.nom as user_nom, u.prenom as user_prenom " +
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
                if (userNom != null && userPrenom != null) {
                    User user = new User();
                    user.setId(userId);
                    user.setNom(userNom);
                    user.setPrenom(userPrenom);
                    commande.setUser(user);
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
                }
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
                stats.add(new Object[]{nomProd, totalVentes});
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }
}