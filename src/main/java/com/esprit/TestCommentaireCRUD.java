package com.esprit;

import com.esprit.models.Sondage;
import com.esprit.models.Commentaire;
import com.esprit.models.User;
import com.esprit.services.SondageService;
import com.esprit.services.UserService;
import com.esprit.services.CommentaireService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

/**
 * Application de test pour les opérations CRUD sur les commentaires des sondages
 */
public class TestCommentaireCRUD {
    private static final Scanner scanner = new Scanner(System.in);
    private static final SondageService sondageService = SondageService.getInstance();
    private static final UserService userService = new UserService();
    private static final CommentaireService commentaireService = new CommentaireService();
    
    // ID statique de l'utilisateur
    private static final int USER_ID = 3;
    
    public static void main(String[] args) {
        System.out.println("Test CRUD pour les commentaires des sondages");
        System.out.println("==========================================");
        
        try {
            // Récupérer l'utilisateur statique
            User user = userService.getById(USER_ID);
            if (user == null) {
                System.err.println("Erreur: L'utilisateur avec ID=" + USER_ID + " n'existe pas dans la base de données.");
                return;
            }
            
            System.out.println("Utilisateur: " + user.getPrenom() + " " + user.getNom() + " (ID: " + user.getId() + ")");
            
            boolean continuer = true;
            while (continuer) {
                System.out.println("\nMenu principal:");
                System.out.println("1. Afficher tous les sondages disponibles");
                System.out.println("2. Ajouter un commentaire à un sondage");
                System.out.println("3. Modifier un de mes commentaires");
                System.out.println("4. Supprimer un de mes commentaires");
                System.out.println("5. Afficher tous les commentaires d'un sondage");
                System.out.println("6. Afficher tous mes commentaires");
                System.out.println("7. Quitter");
                System.out.print("\nChoisissez une option (1-7): ");
                
                int choix = lireEntier();
                
                switch (choix) {
                    case 1:
                        afficherSondages();
                        break;
                    case 2:
                        ajouterCommentaire(user);
                        break;
                    case 3:
                        modifierCommentaire(user);
                        break;
                    case 4:
                        supprimerCommentaire(user);
                        break;
                    case 5:
                        afficherCommentairesSondage();
                        break;
                    case 6:
                        afficherMesCommentaires(user);
                        break;
                    case 7:
                        continuer = false;
                        System.out.println("Au revoir!");
                        break;
                    default:
                        System.out.println("Option invalide, veuillez réessayer.");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
    
    /**
     * Affiche tous les sondages disponibles
     */
    private static void afficherSondages() throws SQLException {
        System.out.println("\n=== Liste des sondages disponibles ===");
        
        List<Sondage> sondages = sondageService.getAll();
        
        if (sondages.isEmpty()) {
            System.out.println("Aucun sondage trouvé.");
            return;
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        for (Sondage sondage : sondages) {
            System.out.println("ID: " + sondage.getId() + " - Question: " + sondage.getQuestion());
            System.out.println("  Créé le: " + sondage.getCreatedAt().format(formatter));
            System.out.println("  Club: " + (sondage.getClub() != null ? sondage.getClub().getNom() : "N/A"));
            System.out.println("  Nombre de commentaires: " + commentaireService.getBySondage(sondage.getId()).size());
            System.out.println("-----------------------------------");
        }
    }
    
    /**
     * Ajoute un commentaire à un sondage
     */
    private static void ajouterCommentaire(User user) throws SQLException {
        System.out.println("\n=== Ajouter un commentaire ===");
        
        // Afficher les sondages disponibles
        afficherSondages();
        
        // Demander l'ID du sondage
        System.out.print("\nEntrez l'ID du sondage auquel vous souhaitez ajouter un commentaire: ");
        int sondageId = lireEntier();
        
        // Récupérer le sondage
        Sondage sondage = sondageService.getById(sondageId);
        
        if (sondage == null) {
            System.out.println("Sondage non trouvé.");
            return;
        }
        
        // Demander le contenu du commentaire
        System.out.println("\nQuestion du sondage: " + sondage.getQuestion());
        System.out.print("Entrez votre commentaire: ");
        String contenu = scanner.nextLine();
        
        if (contenu.trim().isEmpty()) {
            System.out.println("Le commentaire ne peut pas être vide. Opération annulée.");
            return;
        }
        
        // Créer le commentaire
        Commentaire commentaire = new Commentaire();
        commentaire.setContenuComment(contenu);
        commentaire.setDateComment(LocalDate.now());
        commentaire.setUser(user);
        commentaire.setSondage(sondage);
        
        // Sauvegarder le commentaire
        commentaireService.add(commentaire);
        
        System.out.println("\nVotre commentaire a été ajouté avec succès!");
    }
    
    /**
     * Modifie un commentaire existant
     */
    private static void modifierCommentaire(User user) throws SQLException {
        System.out.println("\n=== Modifier un commentaire ===");
        
        // Afficher les commentaires de l'utilisateur
        List<Commentaire> mesCommentaires = afficherMesCommentairesAvecId(user);
        
        if (mesCommentaires.isEmpty()) {
            return;
        }
        
        // Demander l'ID du commentaire
        System.out.print("\nEntrez l'ID du commentaire que vous souhaitez modifier: ");
        int commentaireId = lireEntier();
        
        // Vérifier si le commentaire existe et appartient à l'utilisateur
        Commentaire commentaireAModifier = null;
        for (Commentaire c : mesCommentaires) {
            if (c.getId() == commentaireId) {
                commentaireAModifier = c;
                break;
            }
        }
        
        if (commentaireAModifier == null) {
            System.out.println("Commentaire non trouvé ou vous n'êtes pas autorisé à le modifier.");
            return;
        }
        
        // Afficher le contenu actuel
        System.out.println("\nContenu actuel: " + commentaireAModifier.getContenuComment());
        System.out.print("Entrez le nouveau contenu: ");
        String nouveauContenu = scanner.nextLine();
        
        if (nouveauContenu.trim().isEmpty()) {
            System.out.println("Le commentaire ne peut pas être vide. Modification annulée.");
            return;
        }
        
        // Mettre à jour le commentaire
        commentaireAModifier.setContenuComment(nouveauContenu);
        commentaireAModifier.setDateComment(LocalDate.now()); // Mettre à jour la date
        
        // Sauvegarder les modifications
        commentaireService.update(commentaireAModifier);
        
        System.out.println("\nVotre commentaire a été modifié avec succès!");
    }
    
    /**
     * Supprime un commentaire existant
     */
    private static void supprimerCommentaire(User user) throws SQLException {
        System.out.println("\n=== Supprimer un commentaire ===");
        
        // Afficher les commentaires de l'utilisateur
        List<Commentaire> mesCommentaires = afficherMesCommentairesAvecId(user);
        
        if (mesCommentaires.isEmpty()) {
            return;
        }
        
        // Demander l'ID du commentaire
        System.out.print("\nEntrez l'ID du commentaire que vous souhaitez supprimer: ");
        int commentaireId = lireEntier();
        
        // Vérifier si le commentaire existe et appartient à l'utilisateur
        Commentaire commentaireASupprimer = null;
        for (Commentaire c : mesCommentaires) {
            if (c.getId() == commentaireId) {
                commentaireASupprimer = c;
                break;
            }
        }
        
        if (commentaireASupprimer == null) {
            System.out.println("Commentaire non trouvé ou vous n'êtes pas autorisé à le supprimer.");
            return;
        }
        
        // Demander confirmation
        System.out.print("Êtes-vous sûr de vouloir supprimer ce commentaire ? (o/n): ");
        String confirmation = scanner.nextLine().toLowerCase();
        
        if (confirmation.equals("o") || confirmation.equals("oui")) {
            // Supprimer le commentaire
            commentaireService.delete(commentaireASupprimer.getId());
            System.out.println("\nVotre commentaire a été supprimé avec succès!");
        } else {
            System.out.println("Suppression annulée.");
        }
    }
    
    /**
     * Affiche tous les commentaires d'un sondage
     */
    private static void afficherCommentairesSondage() throws SQLException {
        System.out.println("\n=== Commentaires d'un sondage ===");
        
        // Afficher les sondages disponibles
        afficherSondages();
        
        // Demander l'ID du sondage
        System.out.print("\nEntrez l'ID du sondage dont vous souhaitez voir les commentaires: ");
        int sondageId = lireEntier();
        
        // Récupérer le sondage
        Sondage sondage = sondageService.getById(sondageId);
        
        if (sondage == null) {
            System.out.println("Sondage non trouvé.");
            return;
        }
        
        // Récupérer et afficher les commentaires
        List<Commentaire> commentaires = commentaireService.getBySondage(sondageId);
        
        if (commentaires.isEmpty()) {
            System.out.println("Aucun commentaire pour ce sondage.");
            return;
        }
        
        System.out.println("\nQuestion du sondage: " + sondage.getQuestion());
        System.out.println("Commentaires:");
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        for (Commentaire c : commentaires) {
            System.out.println("-----------------------------------");
            System.out.println("De: " + c.getUser().getPrenom() + " " + c.getUser().getNom());
            System.out.println("Le: " + c.getDateComment().format(formatter));
            System.out.println("Contenu: " + c.getContenuComment());
        }
        System.out.println("-----------------------------------");
    }
    
    /**
     * Affiche tous les commentaires de l'utilisateur
     */
    private static void afficherMesCommentaires(User user) throws SQLException {
        System.out.println("\n=== Mes commentaires ===");
        
        // Récupérer tous les commentaires et filtrer ceux de l'utilisateur
        List<Commentaire> commentaires = commentaireService.getAllComments();
        List<Commentaire> mesCommentaires = commentaires.stream()
            .filter(c -> c.getUser().getId() == user.getId())
            .toList();
        
        if (mesCommentaires.isEmpty()) {
            System.out.println("Vous n'avez fait aucun commentaire.");
            return;
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        for (Commentaire c : mesCommentaires) {
            System.out.println("-----------------------------------");
            System.out.println("ID: " + c.getId());
            System.out.println("Sondage: " + c.getSondage().getQuestion());
            System.out.println("Date: " + c.getDateComment().format(formatter));
            System.out.println("Contenu: " + c.getContenuComment());
        }
        System.out.println("-----------------------------------");
    }
    
    /**
     * Affiche les commentaires de l'utilisateur avec leurs IDs et les retourne
     */
    private static List<Commentaire> afficherMesCommentairesAvecId(User user) throws SQLException {
        System.out.println("\nVos commentaires:");
        
        // Récupérer tous les commentaires et filtrer ceux de l'utilisateur
        List<Commentaire> commentaires = commentaireService.getAllComments();
        List<Commentaire> mesCommentaires = commentaires.stream()
            .filter(c -> c.getUser().getId() == user.getId())
            .toList();
        
        if (mesCommentaires.isEmpty()) {
            System.out.println("Vous n'avez fait aucun commentaire.");
            return mesCommentaires;
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        for (Commentaire c : mesCommentaires) {
            System.out.println("-----------------------------------");
            System.out.println("ID: " + c.getId());
            System.out.println("Sondage: " + c.getSondage().getQuestion());
            System.out.println("Date: " + c.getDateComment().format(formatter));
            System.out.println("Contenu: " + c.getContenuComment());
        }
        System.out.println("-----------------------------------");
        
        return mesCommentaires;
    }
    
    /**
     * Utilitaire pour lire un entier depuis la console
     */
    private static int lireEntier() {
        while (true) {
            try {
                return Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.print("Veuillez entrer un nombre valide: ");
            }
        }
    }
} 