package com.esprit.services;

import com.esprit.models.Commande;
import com.esprit.models.Orderdetails;
import com.esprit.models.enums.StatutCommandeEnum;
import com.esprit.utils.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommandeService {

    private Connection connection;

    public CommandeService() {
        this.connection = DataSource.getInstance().getCnx();
    }

    public void createCommande(Commande commande) {
        try {
            String insertCommandeSQL = "INSERT INTO commande (date_comm, statut, user_id, total) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = connection.prepareStatement(insertCommandeSQL, Statement.RETURN_GENERATED_KEYS);
            ps.setDate(1, Date.valueOf(commande.getDateComm()));
            ps.setString(2, commande.getStatut().name());
            ps.setInt(3, commande.getUser().getId());
            ps.setDouble(4, commande.getTotal());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int commandeId = rs.getInt(1);

                for (Orderdetails detail : commande.getOrderDetails()) {
                    String insertDetailSQL = "INSERT INTO order_details (commande_id, produit_id, quantity, price, total) VALUES (?, ?, ?, ?, ?)";
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
            String deleteDetailsSQL = "DELETE FROM order_details WHERE commande_id = ?";
            PreparedStatement ps1 = connection.prepareStatement(deleteDetailsSQL);
            ps1.setInt(1, id);
            ps1.executeUpdate();

            String deleteCommandeSQL = "DELETE FROM commande WHERE id = ?";
            PreparedStatement ps2 = connection.prepareStatement(deleteCommandeSQL);
            ps2.setInt(1, id);
            ps2.executeUpdate();

            System.out.println("Commande supprimée.");
        } catch (SQLException e) {
            e.printStackTrace();
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
            String query = "SELECT * FROM commande";
            if (keyword != null && !keyword.isEmpty()) {
                query += " WHERE statut LIKE ?";
            }
            query += " ORDER BY date_comm DESC";

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
                // TODO : récupérer l'utilisateur et les orderDetails avec d'autres services
                commandes.add(commande);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return commandes;
    }

    public List<Object[]> getTopProduits() {
        List<Object[]> stats = new ArrayList<>();
        try {
            String sql = "SELECT p.nom_prod, SUM(od.quantity) as total_ventes " +
                    "FROM produit p " +
                    "JOIN order_details od ON p.id = od.produit_id " +
                    "GROUP BY p.id " +
                    "ORDER BY total_ventes DESC";
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                String nomProd = rs.getString("nom_prod");
                int totalVentes = rs.getInt("total_ventes");
                stats.add(new Object[]{nomProd, totalVentes});
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }
}
