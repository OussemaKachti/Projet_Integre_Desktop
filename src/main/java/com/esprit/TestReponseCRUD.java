package com.esprit;

import com.esprit.models.Sondage;
import com.esprit.models.ChoixSondage;
import com.esprit.models.Reponse;
import com.esprit.models.User;
import com.esprit.models.Commentaire;
import com.esprit.services.SondageService;
import com.esprit.services.UserService;
import com.esprit.services.ReponseService;
import com.esprit.services.ChoixSondageService;
import com.esprit.services.CommentaireService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.Map;

/**
 * Application de test pour les opérations CRUD sur les réponses aux sondages
 */
public class TestReponseCRUD {
    private static final Scanner scanner = new Scanner(System.in);
    private static final SondageService sondageService = SondageService.getInstance();
    private static final UserService userService = new UserService();
    private static final ReponseService reponseService = new ReponseService();
    private static final ChoixSondageService choixService = new ChoixSondageService();
    private static final CommentaireService commentaireService = new CommentaireService();
    
    // ID statique de l'utilisateur
    private static final int USER_ID = 3;
    
    public static void main(String[] args) {
        System.out.println("Test CRUD pour les réponses aux sondages");
        System.out.println("=======================================");
        
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
                System.out.println("2. Répondre à un sondage");
                System.out.println("3. Modifier ma réponse à un sondage");
                System.out.println("4. Supprimer ma réponse à un sondage");
                System.out.println("5. Afficher les résultats d'un sondage");
                System.out.println("6. Afficher mes réponses");
                System.out.println("7. Afficher les sondages d'un club spécifique");
                System.out.println("8. Quitter");
                System.out.print("\nChoisissez une option (1-8): ");
                
                int choix = lireEntier();
                
                switch (choix) {
                    case 1:
                        afficherSondages();
                        break;
                    case 2:
                        repondreAuSondage(user);
                        break;
                    case 3:
                        modifierReponse(user);
                        break;
                    case 4:
                        supprimerReponse(user);
                        break;
                    case 5:
                        afficherResultatsSondage();
                        break;
                    case 6:
                        afficherMesReponses(user);
                        break;
                    case 7:
                        afficherSondagesParClub();
                        break;
                    case 8:
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
     * Affiche tous les sondages disponibles avec pourcentages et couleurs
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
            System.out.println("\nID: " + sondage.getId());
            System.out.println("Question: " + sondage.getQuestion());
            System.out.println("Créé le: " + sondage.getCreatedAt().format(formatter));
            System.out.println("Club: " + sondage.getClub().getNom());
            
            // Récupérer les résultats du sondage pour afficher les pourcentages
            Map<String, Object> results = reponseService.getPollResults(sondage.getId());
            
            System.out.println("Choix:");
            
            // Calculer le nombre total de réponses pour ce sondage
            int totalVotes = 0;
            try {
                for (ChoixSondage choix : sondage.getChoix()) {
                    totalVotes += choixService.getResponseCount(choix.getId());
                }
            } catch (SQLException e) {
                System.err.println("Erreur lors du calcul des votes: " + e.getMessage());
            }
            
            for (ChoixSondage choix : sondage.getChoix()) {
                String choixInfo = "  - " + choix.getId() + ". " + choix.getContenu();
                
                // Ajouter le pourcentage et la couleur
                try {
                    int votes = choixService.getResponseCount(choix.getId());
                    double percentage = totalVotes > 0 ? (votes * 100.0) / totalVotes : 0;
                    String color = getColorByPercentage(percentage);
                    
                    // Formater le pourcentage avec 2 décimales
                    String percentageStr = String.format("%.2f%%", percentage);
                    
                    // Ajouter l'information du pourcentage et la couleur
                    choixInfo += String.format(" (%d votes, %s) %s", votes, percentageStr, color);
                    
                } catch (SQLException e) {
                    choixInfo += " (Erreur de calcul)";
                }
                
                System.out.println(choixInfo);
            }
            
            System.out.println("-----------------------------------");
        }
    }
    
    /**
     * Retourne une représentation de couleur selon le pourcentage
     */
    private static String getColorByPercentage(double percentage) {
        if (percentage <= 20) {
            return "[Rose pâle]"; // Équivalent de '#FFC0CB' - faible participation
        } else if (percentage <= 40) {
            return "[Vert pâle]"; // Équivalent de '#98FB98' - participation modérée basse
        } else if (percentage <= 60) {
            return "[Bleu ciel]"; // Équivalent de '#87CEEB' - participation moyenne
        } else if (percentage <= 80) {
            return "[Vert lime]"; // Équivalent de '#32CD32' - bonne participation
        } else {
            return "[Vert forêt]"; // Équivalent de '#228B22' - excellente participation
        }
    }
    
    /**
     * Répond à un sondage
     */
    private static void repondreAuSondage(User user) throws SQLException {
        System.out.println("\n=== Répondre à un sondage ===");
        
        // Afficher les sondages disponibles
        afficherSondages();
        
        // Demander l'ID du sondage
        System.out.print("\nEntrez l'ID du sondage auquel vous souhaitez répondre: ");
        int sondageId = lireEntier();
        
        // Récupérer le sondage
        Sondage sondage = sondageService.getById(sondageId);
        
        if (sondage == null) {
            System.out.println("Sondage non trouvé.");
            return;
        }
        
        // Vérifier si l'utilisateur a déjà voté pour ce sondage
        if (reponseService.hasUserVoted(user.getId(), sondage.getId())) {
            System.out.println("Vous avez déjà répondu à ce sondage. Vous pouvez modifier votre réponse avec l'option 3.");
            return;
        }
        
        // Afficher les choix du sondage
        System.out.println("\nQuestion: " + sondage.getQuestion());
        System.out.println("Choix disponibles:");
        
        List<ChoixSondage> choix = sondage.getChoix();
        for (int i = 0; i < choix.size(); i++) {
            System.out.println((i + 1) + ". " + choix.get(i).getContenu());
        }
        
        // Demander à l'utilisateur de choisir
        System.out.print("\nEntrez le numéro de votre choix (1-" + choix.size() + "): ");
        int choixIndex = lireEntier() - 1;
        
        if (choixIndex < 0 || choixIndex >= choix.size()) {
            System.out.println("Choix invalide. Réponse annulée.");
            return;
        }
        
        // Créer la réponse
        Reponse reponse = new Reponse();
        reponse.setUser(user);
        reponse.setSondage(sondage);
        reponse.setChoixSondage(choix.get(choixIndex));
        reponse.setDateReponse(LocalDate.now());
        
        // Sauvegarder la réponse
        reponseService.add(reponse);
        
        System.out.println("\nVotre réponse a été enregistrée avec succès!");
    }
    
    /**
     * Modifie une réponse existante
     */
    private static void modifierReponse(User user) throws SQLException {
        System.out.println("\n=== Modifier ma réponse à un sondage ===");
        
        // Afficher les sondages auxquels l'utilisateur a répondu
        afficherMesReponses(user);
        
        // Demander l'ID du sondage
        System.out.print("\nEntrez l'ID du sondage dont vous souhaitez modifier la réponse: ");
        int sondageId = lireEntier();
        
        // Vérifier si l'utilisateur a déjà répondu à ce sondage
        if (!reponseService.hasUserVoted(user.getId(), sondageId)) {
            System.out.println("Vous n'avez pas encore répondu à ce sondage. Utilisez l'option 2 pour répondre.");
            return;
        }
        
        // Récupérer le sondage
        Sondage sondage = sondageService.getById(sondageId);
        if (sondage == null) {
            System.out.println("Sondage non trouvé.");
            return;
        }
        
        // Récupérer la réponse actuelle
        Reponse reponseActuelle = reponseService.getUserResponseForPoll(user.getId(), sondageId);
        
        // Afficher les choix du sondage
        System.out.println("\nQuestion: " + sondage.getQuestion());
        System.out.println("Votre réponse actuelle: " + reponseActuelle.getChoixSondage().getContenu());
        System.out.println("\nChoix disponibles:");
        
        List<ChoixSondage> choix = sondage.getChoix();
        for (int i = 0; i < choix.size(); i++) {
            ChoixSondage choixOption = choix.get(i);
            String marker = choixOption.getId() == reponseActuelle.getChoixSondage().getId() ? " (votre choix actuel)" : "";
            System.out.println((i + 1) + ". " + choixOption.getContenu() + marker);
        }
        
        // Demander à l'utilisateur de choisir
        System.out.print("\nEntrez le numéro de votre nouveau choix (1-" + choix.size() + "): ");
        int choixIndex = lireEntier() - 1;
        
        if (choixIndex < 0 || choixIndex >= choix.size()) {
            System.out.println("Choix invalide. Modification annulée.");
            return;
        }
        
        // Mettre à jour la réponse
        reponseActuelle.setChoixSondage(choix.get(choixIndex));
        reponseActuelle.setDateReponse(LocalDate.now());
        
        // Sauvegarder la modification
        reponseService.update(reponseActuelle);
        
        System.out.println("\nVotre réponse a été modifiée avec succès!");
    }
    
    /**
     * Supprime une réponse existante
     */
    private static void supprimerReponse(User user) throws SQLException {
        System.out.println("\n=== Supprimer ma réponse à un sondage ===");
        
        // Afficher les sondages auxquels l'utilisateur a répondu
        afficherMesReponses(user);
        
        // Demander l'ID du sondage
        System.out.print("\nEntrez l'ID du sondage dont vous souhaitez supprimer la réponse: ");
        int sondageId = lireEntier();
        
        // Vérifier si l'utilisateur a déjà répondu à ce sondage
        if (!reponseService.hasUserVoted(user.getId(), sondageId)) {
            System.out.println("Vous n'avez pas encore répondu à ce sondage.");
            return;
        }
        
        // Récupérer la réponse
        Reponse reponse = reponseService.getUserResponseForPoll(user.getId(), sondageId);
        if (reponse == null) {
            System.out.println("Réponse non trouvée.");
            return;
        }
        
        // Demander confirmation
        System.out.print("Êtes-vous sûr de vouloir supprimer votre réponse ? (o/n): ");
        String confirmation = scanner.nextLine().toLowerCase();
        
        if (confirmation.equals("o") || confirmation.equals("oui")) {
            // Supprimer la réponse
            reponseService.delete(reponse.getId());
            System.out.println("Votre réponse a été supprimée avec succès!");
        } else {
            System.out.println("Suppression annulée.");
        }
    }
    
    /**
     * Affiche les résultats d'un sondage avec pourcentages et couleurs
     */
    private static void afficherResultatsSondage() throws SQLException {
        System.out.println("\n=== Afficher les résultats d'un sondage ===");
        
        // Afficher les sondages disponibles
        List<Sondage> sondages = sondageService.getAll();
        
        if (sondages.isEmpty()) {
            System.out.println("Aucun sondage trouvé.");
            return;
        }
        
        for (Sondage s : sondages) {
            System.out.println("ID: " + s.getId() + " - " + s.getQuestion());
        }
        
        // Demander l'ID du sondage
        System.out.print("\nEntrez l'ID du sondage dont vous souhaitez voir les résultats: ");
        int sondageId = lireEntier();
        
        // Récupérer le sondage
        Sondage sondage = sondageService.getById(sondageId);
        
        if (sondage == null) {
            System.out.println("Sondage non trouvé.");
            return;
        }
        
        // Calculer le nombre total de votes
        int totalVotes = 0;
        try {
            for (ChoixSondage choix : sondage.getChoix()) {
                totalVotes += choixService.getResponseCount(choix.getId());
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du calcul des votes: " + e.getMessage());
        }
        
        System.out.println("\nRésultats du sondage: " + sondage.getQuestion());
        System.out.println("Total des votes: " + totalVotes);
        System.out.println("-----------------------------------");
        
        if (totalVotes == 0) {
            System.out.println("Aucune réponse pour ce sondage.");
            return;
        }
        
        // Afficher les résultats pour chaque choix
        for (ChoixSondage choix : sondage.getChoix()) {
            try {
                int votes = choixService.getResponseCount(choix.getId());
                double percentage = (votes * 100.0) / totalVotes;
                String color = getColorByPercentage(percentage);
                
                // Créer une barre visuelle représentant le pourcentage (10 caractères = 100%)
                int barLength = (int)Math.round(percentage / 10);
                StringBuilder bar = new StringBuilder("|");
                for (int i = 0; i < 10; i++) {
                    bar.append(i < barLength ? "█" : " ");
                }
                bar.append("|");
                
                System.out.printf("  %s: %d votes (%.2f%%) %s %s\n", 
                                 choix.getContenu(), 
                                 votes, 
                                 percentage, 
                                 bar.toString(),
                                 color);
            } catch (SQLException e) {
                System.out.println("  " + choix.getContenu() + ": Erreur de calcul");
            }
        }
    }
    
    /**
     * Affiche les réponses de l'utilisateur courant
     */
    private static void afficherMesReponses(User user) throws SQLException {
        System.out.println("\n=== Mes réponses aux sondages ===");
        
        // Cette méthode n'existe pas actuellement, il faudrait l'implémenter dans ReponseService
        // Pour le moment, nous pouvons parcourir tous les sondages et vérifier si l'utilisateur a voté
        List<Sondage> sondages = sondageService.getAll();
        boolean aRepondu = false;
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        for (Sondage sondage : sondages) {
            if (reponseService.hasUserVoted(user.getId(), sondage.getId())) {
                aRepondu = true;
                
                Reponse reponse = reponseService.getUserResponseForPoll(user.getId(), sondage.getId());
                
                System.out.println("\nID du sondage: " + sondage.getId());
                System.out.println("Question: " + sondage.getQuestion());
                System.out.println("Votre réponse: " + reponse.getChoixSondage().getContenu());
                System.out.println("Date de réponse: " + reponse.getDateReponse().format(formatter));
                System.out.println("-----------------------------------");
            }
        }
        
        if (!aRepondu) {
            System.out.println("Vous n'avez répondu à aucun sondage.");
        }
    }
    
    /**
     * Affiche les sondages d'un club spécifique
     */
    private static void afficherSondagesParClub() throws SQLException {
        System.out.println("\n=== Liste des sondages par club ===");
        
        System.out.print("Entrez l'ID ou le nom du club: ");
        String clubId = scanner.nextLine();
        
        // Récupérer les sondages du club
        List<Sondage> sondages = sondageService.getByClub(clubId);
        
        if (sondages.isEmpty()) {
            System.out.println("Aucun sondage trouvé pour ce club.");
            return;
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        for (Sondage sondage : sondages) {
            System.out.println("\nID: " + sondage.getId());
            System.out.println("Question: " + sondage.getQuestion());
            System.out.println("Créé le: " + sondage.getCreatedAt().format(formatter));
            
            // Calculer le nombre total de réponses pour ce sondage
            int totalVotes = 0;
            try {
                for (ChoixSondage choix : sondage.getChoix()) {
                    totalVotes += choixService.getResponseCount(choix.getId());
                }
            } catch (SQLException e) {
                System.err.println("Erreur lors du calcul des votes: " + e.getMessage());
            }
            
            System.out.println("Total des votes: " + totalVotes);
            System.out.println("Choix:");
            
            for (ChoixSondage choix : sondage.getChoix()) {
                try {
                    int votes = choixService.getResponseCount(choix.getId());
                    double percentage = totalVotes > 0 ? (votes * 100.0) / totalVotes : 0;
                    String color = getColorByPercentage(percentage);
                    
                    // Créer une barre visuelle représentant le pourcentage (10 caractères = 100%)
                    int barLength = (int)Math.round(percentage / 10);
                    StringBuilder bar = new StringBuilder("|");
                    for (int i = 0; i < 10; i++) {
                        bar.append(i < barLength ? "█" : " ");
                    }
                    bar.append("|");
                    
                    System.out.printf("  %s: %d votes (%.2f%%) %s %s\n", 
                                     choix.getContenu(), 
                                     votes, 
                                     percentage, 
                                     bar.toString(),
                                     color);
                } catch (SQLException e) {
                    System.out.println("  " + choix.getContenu() + ": Erreur de calcul");
                }
            }
            
            // Afficher les commentaires du sondage s'il y en a
            try {
                List<Commentaire> commentaires = commentaireService.getBySondage(sondage.getId());
                if (!commentaires.isEmpty()) {
                    System.out.println("\nCommentaires:");
                    for (Commentaire c : commentaires) {
                        System.out.printf("  %s [%s]: %s\n", 
                                         c.getUser().getPrenom() + " " + c.getUser().getNom(),
                                         c.getDateComment().format(formatter),
                                         c.getContenuComment());
                    }
                }
            } catch (Exception e) {
                System.out.println("Impossible de charger les commentaires: " + e.getMessage());
            }
            
            System.out.println("-----------------------------------");
        }
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