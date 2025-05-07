package com.esprit.controllers;

import com.esprit.models.ParticipationMembre;
import com.esprit.services.ParticipationMembreService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ShowParticipantController {

    @FXML private TableView<ParticipationMembre> participantTable;
    @FXML private TableColumn<ParticipationMembre, Integer> idColumn;
    @FXML private TableColumn<ParticipationMembre, String> nameColumn;
    @FXML private TableColumn<ParticipationMembre, String> clubNameColumn;
    @FXML private TableColumn<ParticipationMembre, String> dateRequestColumn;
    @FXML private TableColumn<ParticipationMembre, String> statutColumn;
    @FXML private TextField searchField;
    @FXML private Pagination pagination;

    private final ParticipationMembreService participantService = new ParticipationMembreService();
    private List<ParticipationMembre> allAcceptedParticipants = new ArrayList<>();
    private List<ParticipationMembre> filteredParticipants = new ArrayList<>();
    private static final int ITEMS_PER_PAGE = 3;

    @FXML
    public void initialize() {
        // Set up table columns
        idColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getId()).asObject());
        nameColumn.setCellValueFactory(cellData -> {
            ParticipationMembre p = cellData.getValue();
            return new SimpleStringProperty(p.getUser() != null ? p.getUser().getFullName() : "N/A");
        });
        clubNameColumn.setCellValueFactory(cellData -> {
            ParticipationMembre p = cellData.getValue();
            return new SimpleStringProperty(p.getClub() != null ? p.getClub().getNomC() : "N/A");
        });
        dateRequestColumn.setCellValueFactory(cellData -> {
            ParticipationMembre p = cellData.getValue();
            return new SimpleStringProperty(p.getDateRequest() != null ? p.getDateRequest().toString() : "N/A");
        });
        statutColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatut()));

        // Add listener for pagination changes
        pagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> {
            updateTable();
        });

        // Add listener for search field changes
        searchField.setOnAction(event -> {
            System.out.println("Search field action triggered.");
            searchParticipants();
        });

        // Load participants and set up pagination
        loadAcceptedParticipants();
    }

    @FXML
    private void loadAcceptedParticipants() {
        System.out.println("Refresh button clicked - Starting loadAcceptedParticipants");
        try {
            // Fetch all participants
            System.out.println("Fetching all participants from ParticipantService...");
            List<ParticipationMembre> allParticipants = participantService.getAllParticipants();
            System.out.println("Total participants fetched: " + allParticipants.size());
            allParticipants.forEach(participant -> {
                System.out.println("Participant: " + participant.getId() + 
                        ", Name: " + (participant.getUser() != null ? participant.getUser().getFullName() : "null") +
                        ", Statut: " + participant.getStatut() +
                        ", Club: " + (participant.getClub() != null ? participant.getClub().getNomC() : "null") +
                        ", Date Request: " + (participant.getDateRequest() != null ? participant.getDateRequest().toString() : "null"));
            });

            // Filter for accepted participants
            System.out.println("Filtering for accepted participants...");
            allAcceptedParticipants = allParticipants.stream()
                    .filter(participant -> "accepte".equalsIgnoreCase(participant.getStatut()))
                    .collect(Collectors.toList());
            System.out.println("Accepted participants loaded: " + allAcceptedParticipants.size());

            // Initialize filtered list with all accepted participants
            filteredParticipants = new ArrayList<>(allAcceptedParticipants);
            searchField.setText(""); // Reset search field
            System.out.println("Search field reset.");

            // Set up pagination
            System.out.println("Setting up pagination...");
            setupPagination();
            System.out.println("Updating table...");
            updateTable();
            System.out.println("loadAcceptedParticipants completed successfully.");
        } catch (SQLException e) {
            System.err.println("SQLException in loadAcceptedParticipants: " + e.getMessage());
            e.printStackTrace();
            showError("Error loading participants: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error in loadAcceptedParticipants: " + e.getMessage());
            e.printStackTrace();
            showError("Unexpected error: " + e.getMessage());
        }
    }

    @FXML
    private void searchParticipants() {
        System.out.println("searchParticipants method called.");
        String searchText = searchField.getText().trim().toLowerCase();
        System.out.println("Search initiated with text: " + searchText);
        try {
            if (searchText.isEmpty()) {
                System.out.println("Search text is empty, resetting to all accepted participants.");
                filteredParticipants = new ArrayList<>(allAcceptedParticipants);
            } else {
                System.out.println("Filtering participants by club name: " + searchText);
                filteredParticipants = allAcceptedParticipants.stream()
                        .filter(participant -> {
                            if (participant.getClub() == null) {
                                System.out.println("Participant " + participant.getId() + " has null club, skipping.");
                                return false;
                            }
                            String clubName = participant.getClub().getNomC();
                            if (clubName == null) {
                                System.out.println("Participant " + participant.getId() + " has null club name, skipping.");
                                return false;
                            }
                            boolean matches = clubName.toLowerCase().contains(searchText);
                            System.out.println("Participant " + participant.getId() + ", Club: " + clubName + ", Matches: " + matches);
                            return matches;
                        })
                        .collect(Collectors.toList());
            }
            System.out.println("Filtered participants after search: " + filteredParticipants.size());
            if (filteredParticipants.isEmpty()) {
                System.out.println("No participants match the search criteria.");
            } else {
                filteredParticipants.forEach(p -> System.out.println("Filtered: " + p.getId() + ", Club: " + (p.getClub() != null ? p.getClub().getNomC() : "null")));
            }
            setupPagination();
            updateTable();
            System.out.println("searchParticipants completed successfully.");
        } catch (Exception e) {
            System.err.println("Error in searchParticipants: " + e.getMessage());
            e.printStackTrace();
            showError("Error during search: " + e.getMessage());
        }
    }

    private void setupPagination() {
        System.out.println("Setting up pagination with " + filteredParticipants.size() + " participants.");
        if (filteredParticipants.isEmpty()) {
            pagination.setPageCount(1);
            pagination.setCurrentPageIndex(0);
        } else {
            int pageCount = (int) Math.ceil((double) filteredParticipants.size() / ITEMS_PER_PAGE);
            pagination.setPageCount(pageCount);
            pagination.setCurrentPageIndex(0); // Reset to first page after search
        }
        System.out.println("Pagination set with page count: " + pagination.getPageCount());
    }

    private void updateTable() {
        int currentPage = pagination.getCurrentPageIndex();
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filteredParticipants.size());

        ObservableList<ParticipationMembre> pageParticipants = FXCollections.observableArrayList();
        if (startIndex < filteredParticipants.size()) {
            pageParticipants.addAll(filteredParticipants.subList(startIndex, endIndex));
        }

        participantTable.setItems(pageParticipants);
        System.out.println("Table updated - Page " + (currentPage + 1) + ": Showing participants " +
                (startIndex + 1) + " to " + endIndex + " of " + filteredParticipants.size());
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}