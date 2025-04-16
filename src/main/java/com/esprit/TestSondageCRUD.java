package com.esprit;

import com.esprit.models.Sondage;
import com.esprit.models.ChoixSondage;
import com.esprit.models.User;
import com.esprit.models.Club;
import com.esprit.services.SondageService;
import com.esprit.services.UserService;
import com.esprit.services.ClubService;
import com.esprit.services.ChoixSondageService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.ArrayList;

/**
 * Application de test pour les opérations CRUD sur les sondages
 */
public class TestSondageCRUD {
    private static final Scanner scanner = new Scanner(System.in);
    private static final SondageService sondageService = SondageService.getInstance();
    private static final UserService userService = new UserService();
    private static final ClubService clubService = new ClubService();
    private static final ChoixSondageService choixService = new ChoixSondageService();
    
    // IDs statiques
    private static final int USER_ID = 2;
    private static final int CLUB_ID = 1;

    public static void main(String[] args) {
        System.out.println("Test CRUD pour les sondages");
        System.out.println("==========================");
        
        try {
            // Récupérer l'utilisateur et le club statiques
            User user = userService.getById(USER_ID);
            if (user == null) {
                System.err.println("Erreur: L'utilisateur avec ID=" + USER_ID + " n'existe pas dans la base de données.");
                return;
            }
            
            Club club = clubService.getById(CLUB_ID);
            if (club == null) {
                System.err.println("Erreur: Le club avec ID=" + CLUB_ID + " n'existe pas dans la base de données.");
                return;
            }
            
            System.out.println("Utilisateur: " + user.getPrenom() + " " + user.getNom() + " (ID: " + user.getId() + ")");
            System.out.println("Club: " + club.getNomC() + " (ID: " + club.getId() + ")");
            
            boolean continuer = true;
            while (continuer) {
                System.out.println("\nMenu principal:");
                System.out.println("1. Afficher tous les sondages du club");
                System.out.println("2. Créer un nouveau sondage");
                System.out.println("3. Modifier un sondage existant");
                System.out.println("4. Supprimer un sondage existant");
                System.out.println("5. Quitter");
                System.out.print("\nChoisissez une option (1-5): ");
                
                int choix = lireEntier();
                
                switch (choix) {
                    case 1:
                        afficherSondages(CLUB_ID);
                        break;
                    case 2:
                        creerSondage(user, club);
                        break;
                    case 3:
                        modifierSondage(user, club);
                        break;
                    case 4:
                        supprimerSondage();
                        break;
                    case 5:
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
     * Affiche tous les sondages d'un club spécifique
     */
    private static void afficherSondages(int clubId) throws SQLException {
        System.out.println("\n=== Liste des sondages du club ===");
        
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
            System.out.println("Créé par: " + sondage.getUser().getPrenom() + " " + sondage.getUser().getNom());
            
            System.out.println("Choix:");
            for (ChoixSondage choix : sondage.getChoix()) {
                System.out.println("  - " + choix.getContenu());
            }
            
            System.out.println("-----------------------------------");
        }
    }
    
    /**
     * Crée un nouveau sondage avec des choix
     */
    private static void creerSondage(User user, Club club) throws SQLException {
        System.out.println("\n=== Création d'un nouveau sondage ===");
        
        // Saisir la question
        System.out.print("Question du sondage: ");
        String question = scanner.nextLine();
        
        if (question.trim().isEmpty()) {
            System.out.println("La question ne peut pas être vide. Création annulée.");
            return;
        }
        
        // Créer le sondage
        Sondage sondage = new Sondage();
        sondage.setQuestion(question);
        sondage.setCreatedAt(LocalDateTime.now());
        sondage.setUser(user);
        sondage.setClub(club);
        
        // Ajouter les choix
        System.out.println("\nAjout des choix (entrez une ligne vide pour terminer)");
        
        List<String> choix = new ArrayList<>();
        int choixCount = 1;
        
        while (true) {
            System.out.print("Choix " + choixCount + ": ");
            String contenu = scanner.nextLine();
            
            if (contenu.trim().isEmpty()) {
                break;
            }
            
            choix.add(contenu);
            choixCount++;
        }
        
        if (choix.isEmpty()) {
            System.out.println("Au moins un choix est requis. Création annulée.");
            return;
        }
        
        // Ajouter les choix au sondage
        for (String contenuChoix : choix) {
            ChoixSondage choixSondage = new ChoixSondage();
            choixSondage.setContenu(contenuChoix);
            sondage.addChoix(choixSondage);
        }
        
        // Sauvegarder le sondage
        sondageService.add(sondage);
        
        System.out.println("\nSondage créé avec succès!");
        System.out.println("ID du sondage: " + sondage.getId());
    }
    
    /**
     * Modifie un sondage existant
     */
    private static void modifierSondage(User user, Club club) throws SQLException {
        System.out.println("\n=== Modification d'un sondage ===");
        
        // Afficher les sondages disponibles
        afficherSondages(CLUB_ID);
        
        // Demander l'ID du sondage à modifier
        System.out.print("\nEntrez l'ID du sondage à modifier: ");
        int sondageId = lireEntier();
        
        // Récupérer le sondage
        Sondage sondage = sondageService.getById(sondageId);
        
        if (sondage == null) {
            System.out.println("Sondage non trouvé.");
            return;
        }
        
        // Vérifier si l'utilisateur est l'auteur du sondage
        if (sondage.getUser().getId() != user.getId()) {
            System.out.println("Vous ne pouvez modifier que vos propres sondages.");
            return;
        }
        
        // Afficher les informations actuelles
        System.out.println("Question actuelle: " + sondage.getQuestion());
        
        // Saisir la nouvelle question
        System.out.print("Nouvelle question (laisser vide pour conserver l'actuelle): ");
        String nouvelleQuestion = scanner.nextLine();
        
        if (!nouvelleQuestion.trim().isEmpty()) {
            sondage.setQuestion(nouvelleQuestion);
        }
        
        // Afficher les choix actuels
        System.out.println("\nChoix actuels:");
        List<ChoixSondage> choixActuels = sondage.getChoix();
        for (int i = 0; i < choixActuels.size(); i++) {
            System.out.println((i + 1) + ". " + choixActuels.get(i).getContenu());
        }
        
        // Menu pour la modification des choix
        System.out.println("\nOptions pour les choix:");
        System.out.println("1. Modifier les choix existants");
        System.out.println("2. Ajouter de nouveaux choix");
        System.out.println("3. Ne pas modifier les choix");
        System.out.print("Choisissez une option (1-3): ");
        
        int optionChoix = lireEntier();
        
        // Gérer la modification des choix existants
        if (optionChoix == 1) {
            modifierChoixExistants(choixActuels);
        } 
        // Gérer l'ajout de nouveaux choix
        else if (optionChoix == 2) {
            ajouterNouveauxChoix(sondage);
        }
        
        // Sauvegarder les modifications
        sondageService.update(sondage);
        
        System.out.println("Sondage modifié avec succès!");
    }
    
    /**
     * Modifie les choix existants d'un sondage
     */
    private static void modifierChoixExistants(List<ChoixSondage> choix) throws SQLException {
        System.out.println("\n=== Modification des choix existants ===");
        
        for (int i = 0; i < choix.size(); i++) {
            ChoixSondage choixActuel = choix.get(i);
            System.out.println("Choix " + (i + 1) + ": " + choixActuel.getContenu());
            System.out.print("Nouveau contenu (laisser vide pour conserver l'actuel): ");
            
            String nouveauContenu = scanner.nextLine();
            if (!nouveauContenu.trim().isEmpty()) {
                choixActuel.setContenu(nouveauContenu);
                choixService.update(choixActuel);
                System.out.println("Choix modifié.");
            }
        }
    }
    
    /**
     * Ajoute de nouveaux choix à un sondage existant
     */
    private static void ajouterNouveauxChoix(Sondage sondage) throws SQLException {
        System.out.println("\n=== Ajout de nouveaux choix ===");
        System.out.println("Entrez les nouveaux choix (ligne vide pour terminer)");
        
        int choixCount = sondage.getChoix().size() + 1;
        boolean choixAjoutes = false;
        
        while (true) {
            System.out.print("Nouveau choix " + choixCount + ": ");
            String contenu = scanner.nextLine();
            
            if (contenu.trim().isEmpty()) {
                break;
            }
            
            ChoixSondage nouveauChoix = new ChoixSondage();
            nouveauChoix.setContenu(contenu);
            nouveauChoix.setSondage(sondage);
            
            // Ajouter à la base de données
            choixService.add(nouveauChoix);
            
            // Ajouter à la liste des choix du sondage
            sondage.getChoix().add(nouveauChoix);
            
            choixCount++;
            choixAjoutes = true;
        }
        
        if (choixAjoutes) {
            System.out.println("Nouveaux choix ajoutés avec succès.");
        }
    }
    
    /**
     * Supprime un sondage existant
     */
    private static void supprimerSondage() throws SQLException {
        System.out.println("\n=== Suppression d'un sondage ===");
        
        // Afficher les sondages disponibles
        afficherSondages(CLUB_ID);
        
        // Demander l'ID du sondage à supprimer
        System.out.print("\nEntrez l'ID du sondage à supprimer: ");
        int sondageId = lireEntier();
        
        // Récupérer le sondage
        Sondage sondage = sondageService.getById(sondageId);
        
        if (sondage == null) {
            System.out.println("Sondage non trouvé.");
            return;
        }
        
        // Demander confirmation
        System.out.print("Êtes-vous sûr de vouloir supprimer ce sondage ? (o/n): ");
        String confirmation = scanner.nextLine().toLowerCase();
        
        if (confirmation.equals("o") || confirmation.equals("oui")) {
            sondageService.delete(sondageId);
            System.out.println("Sondage supprimé avec succès!");
        } else {
            System.out.println("Suppression annulée.");
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