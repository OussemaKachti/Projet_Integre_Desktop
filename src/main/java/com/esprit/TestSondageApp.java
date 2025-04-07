package com.esprit;

import com.esprit.models.Sondage;
import com.esprit.models.ChoixSondage;
import com.esprit.models.User;
import com.esprit.models.Club;
import com.esprit.services.SondageService;
import com.esprit.services.UserService;
import com.esprit.services.ClubService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Scanner;

/**
 * Application de test pour l'ajout d'un sondage
 */
public class TestSondageApp {

    public static void main(String[] args) {
        System.out.println("Test d'ajout d'un sondage");
        System.out.println("=========================");
        
        try {
            // Instancier les services
            SondageService sondageService = SondageService.getInstance();
            UserService userService = new UserService();
            ClubService clubService = new ClubService();
            
            // 1. Récupérer l'utilisateur statique avec ID=2
            User user = userService.getById(2);
            if (user == null) {
                System.err.println("Erreur: L'utilisateur avec ID=2 n'existe pas dans la base de données.");
                return;
            }
            System.out.println("Utilisateur récupéré: " + user.getPrenom() + " " + user.getNom());
            
            // 2. Récupérer le club statique avec ID=1
            Club club = clubService.getById(1);
            if (club == null) {
                System.err.println("Erreur: Le club avec ID=1 n'existe pas dans la base de données.");
                return;
            }
            System.out.println("Club récupéré: " + club.getNom() + " (ID: " + club.getId() + ")");
            
            // 3. Créer un nouveau sondage avec des entrées utilisateur
            Scanner scanner = new Scanner(System.in);
            
            System.out.println("\nCréation d'un nouveau sondage");
            System.out.print("Question du sondage: ");
            String question = scanner.nextLine();
            
            Sondage sondage = new Sondage();
            sondage.setQuestion(question);
            sondage.setCreatedAt(LocalDateTime.now());
            sondage.setUser(user);
            sondage.setClub(club);
            
            // 4. Ajouter des choix au sondage
            System.out.println("\nAjout des choix (entrez une ligne vide pour terminer)");
            int choixCount = 1;
            while (true) {
                System.out.print("Choix " + choixCount + ": ");
                String contenu = scanner.nextLine();
                
                if (contenu.trim().isEmpty()) {
                    break;
                }
                
                ChoixSondage choix = new ChoixSondage();
                choix.setContenu(contenu);
                sondage.addChoix(choix);
                
                choixCount++;
            }
            
            // Vérifier qu'il y a au moins un choix
            if (sondage.getChoix().isEmpty()) {
                System.err.println("Erreur: Au moins un choix est requis pour créer un sondage.");
                return;
            }
            
            // 5. Sauvegarder le sondage
            sondageService.add(sondage);
            
            System.out.println("\nSondage créé avec succès!");
            System.out.println("ID du sondage: " + sondage.getId());
            System.out.println("Question: " + sondage.getQuestion());
            System.out.println("Créé par: " + user.getPrenom() + " " + user.getNom() + " (ID: " + user.getId() + ")");
            System.out.println("Club: " + club.getNom() + " (ID: " + club.getId() + ")");
            System.out.println("Choix:");
            for (ChoixSondage choix : sondage.getChoix()) {
                System.out.println("- " + choix.getContenu() + " (ID: " + choix.getId() + ")");
            }
            
            scanner.close();
            
        } catch (SQLException e) {
            System.err.println("Erreur SQL: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Erreur inattendue: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 