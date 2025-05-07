package com.esprit.controllers;

import com.esprit.models.*;
import com.esprit.models.enums.GoalTypeEnum;
import com.esprit.services.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.util.StringConverter;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CompetitionStatisticsController implements Initializable {

    @FXML private ComboBox<Club> clubComboBox;
    @FXML private ComboBox<Saison> seasonComboBox;
    @FXML private ComboBox<Competition> competitionComboBox;

    // Overview Cards
    @FXML private Text totalClubsText;
    @FXML private Text totalCompetitionsText;
    @FXML private Text totalPointsDistributedText;
    @FXML private Text averageCompletionRateText;

    // Club Statistics
    @FXML private Text clubTotalPointsText;
    @FXML private Text clubCompletedMissionsText;
    @FXML private Text clubTotalMissionsText;
    @FXML private Text clubCompletionRateText;
    @FXML private Text clubCurrentRankText;
    @FXML private PieChart clubMissionTypesChart;
    @FXML private LineChart<String, Number> clubPerformanceChart;

    // Season Statistics
    @FXML private Text seasonTotalCompetitionsText;
    @FXML private Text seasonActiveCompetitionsText;
    @FXML private Text seasonCompletedCompetitionsText;
    @FXML private Text seasonTotalPointsText;
    @FXML private BarChart<String, Number> seasonTopClubsChart;
    @FXML private PieChart seasonCompetitionTypesChart;

    // Competition Statistics
    @FXML private Text competitionParticipatingClubsText;
    @FXML private Text competitionCompletedByClubsText;
    @FXML private Text competitionAverageProgressText;
    @FXML private Text competitionCompletionRateText;
    @FXML private BarChart<String, Number> competitionProgressChart;

    // Leaderboard
    @FXML private TableView<Club> leaderboardTable;
    @FXML private TableColumn<Club, Integer> rankColumn;
    @FXML private TableColumn<Club, String> clubNameColumn;
    @FXML private TableColumn<Club, Integer> pointsColumn;

    // Active Missions
    @FXML private ListView<Competition> activeMissionsListView;
    @FXML private Text totalActiveMissionsText;
    @FXML private Text totalPointsAvailableText;
    @FXML private PieChart missionTypeDistributionChart;

    // Add these fields at the top of the class
    @FXML private Label adminNameLabel;
    @FXML private Button userManagementButton;
    @FXML private Button clubManagementButton;
    @FXML private Button eventManagementButton;
    @FXML private Button productOrdersButton;
    @FXML private Button competitionButton;
    @FXML private Button surveyButton;
    @FXML private Button profileButton;
    @FXML private Button logoutButton;

    // Trends
    @FXML private BarChart<String, Number> completionTrendsChart;

    private GamificationStatisticsService statisticsService;
    private ClubService clubService;
    private SaisonService saisonService;
    private CompetitionService competitionService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            // Initialize services
            statisticsService = GamificationStatisticsService.getInstance();
            clubService = ClubService.getInstance();
            saisonService = new SaisonService();
            competitionService = new CompetitionService();

            // Setup UI components
            setupComboBoxes();
            setupLeaderboardTable();
            setupCharts();
            setupListView();

            // Load initial data
            loadOverviewStatistics();
            loadLeaderboard();
            loadActiveMissions();
            loadCompletionTrends();

            // Setup event listeners
            setupEventListeners();

        } catch (Exception e) {
            showError("Error initializing statistics", e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupComboBoxes() throws SQLException {
        // Setup Club ComboBox
        ObservableList<Club> clubs = FXCollections.observableArrayList(clubService.getAll());
        clubComboBox.setItems(clubs);
        clubComboBox.setConverter(new StringConverter<Club>() {
            @Override
            public String toString(Club club) {
                return club != null ? club.getNomC() : "";
            }

            @Override
            public Club fromString(String string) {
                return null;
            }
        });

        // Setup Season ComboBox
        ObservableList<Saison> seasons = FXCollections.observableArrayList(saisonService.getAll());
        seasonComboBox.setItems(seasons);
        seasonComboBox.setConverter(new StringConverter<Saison>() {
            @Override
            public String toString(Saison saison) {
                return saison != null ? saison.getNomSaison() : "";
            }

            @Override
            public Saison fromString(String string) {
                return null;
            }
        });

        // Setup Competition ComboBox
        ObservableList<Competition> competitions = FXCollections.observableArrayList(competitionService.getAll());
        competitionComboBox.setItems(competitions);
        competitionComboBox.setConverter(new StringConverter<Competition>() {
            @Override
            public String toString(Competition competition) {
                return competition != null ? competition.getNomComp() : "";
            }

            @Override
            public Competition fromString(String string) {
                return null;
            }
        });
    }

    private void setupLeaderboardTable() {
        rankColumn.setCellValueFactory(cellData -> {
            int rank = leaderboardTable.getItems().indexOf(cellData.getValue()) + 1;
            return new javafx.beans.property.SimpleIntegerProperty(rank).asObject();
        });

        clubNameColumn.setCellValueFactory(new PropertyValueFactory<>("nomC"));
        pointsColumn.setCellValueFactory(new PropertyValueFactory<>("points"));
    }

    private void setupCharts() {
        // Setup club performance chart
        if (clubPerformanceChart != null) {
            clubPerformanceChart.getXAxis().setLabel("Season");
            clubPerformanceChart.getYAxis().setLabel("Points");
        }

        // Setup season top clubs chart
        if (seasonTopClubsChart != null) {
            seasonTopClubsChart.getXAxis().setLabel("Club");
            seasonTopClubsChart.getYAxis().setLabel("Points");
        }

        // Setup competition progress chart
        if (competitionProgressChart != null) {
            competitionProgressChart.getXAxis().setLabel("Club");
            competitionProgressChart.getYAxis().setLabel("Progress");
        }

        // Setup completion trends chart
        if (completionTrendsChart != null) {
            completionTrendsChart.getXAxis().setLabel("Goal Type");
            completionTrendsChart.getYAxis().setLabel("Completion Rate (%)");
        }
    }

    private void setupListView() {
        if (activeMissionsListView != null) {
            activeMissionsListView.setCellFactory(param -> new ListCell<Competition>() {
                @Override
                protected void updateItem(Competition item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(String.format("%s - %d points", item.getNomComp(), item.getPoints()));
                    }
                }
            });
        }
    }

    private void setupEventListeners() {
        if (clubComboBox != null) {
            clubComboBox.setOnAction(event -> {
                Club selectedClub = clubComboBox.getValue();
                if (selectedClub != null) {
                    loadClubStatistics(selectedClub);
                }
            });
        }

        if (seasonComboBox != null) {
            seasonComboBox.setOnAction(event -> {
                Saison selectedSeason = seasonComboBox.getValue();
                if (selectedSeason != null) {
                    loadSeasonStatistics(selectedSeason);
                }
            });
        }

        if (competitionComboBox != null) {
            competitionComboBox.setOnAction(event -> {
                Competition selectedCompetition = competitionComboBox.getValue();
                if (selectedCompetition != null) {
                    loadCompetitionStatistics(selectedCompetition);
                }
            });
        }
    }

    private void loadOverviewStatistics() {
        try {
            // Load total clubs
            int totalClubs = clubService.getAll().size();
            totalClubsText.setText(String.valueOf(totalClubs));

            // Load total competitions
            int totalCompetitions = competitionService.getAll().size();
            totalCompetitionsText.setText(String.valueOf(totalCompetitions));

            // Calculate total points distributed and average completion rate
            List<Saison> allSeasons = saisonService.getAll();
            int totalPointsDistributed = 0;
            double totalCompletionRate = 0;
            int completionRateCount = 0;

            for (Saison season : allSeasons) {
                GamificationStatisticsService.SeasonStatistics stats = statisticsService.getSeasonStatistics(season.getId());
                if (stats != null) {
                    totalPointsDistributed += stats.getTotalPointsDistributed();

                    if (stats.getTotalCompetitions() > 0) {
                        double completionRate = (double) stats.getCompletedCompetitions() / stats.getTotalCompetitions() * 100;
                        totalCompletionRate += completionRate;
                        completionRateCount++;
                    }
                }
            }

            totalPointsDistributedText.setText(String.valueOf(totalPointsDistributed));

            double averageCompletionRate = completionRateCount > 0 ? totalCompletionRate / completionRateCount : 0;
            averageCompletionRateText.setText(String.format("%.1f%%", averageCompletionRate));

        } catch (SQLException e) {
            showError("Error loading overview statistics", e.getMessage());
        }
    }

    private void loadClubStatistics(Club club) {
        try {
            GamificationStatisticsService.ClubStatistics stats = statisticsService.getClubStatistics(club.getId());

            if (stats != null) {
                clubTotalPointsText.setText(String.valueOf(stats.getTotalPoints()));
                clubCompletedMissionsText.setText(String.valueOf(stats.getCompletedMissions()));
                clubTotalMissionsText.setText(String.valueOf(stats.getTotalMissions()));
                clubCompletionRateText.setText(String.format("%.1f%%", stats.getCompletionRate()));
                clubCurrentRankText.setText("#" + stats.getCurrentRank());

                // Load mission types chart
                loadClubMissionTypesChart(stats);

                // Load performance chart
                loadClubPerformanceChart(club.getId());
            }
        } catch (SQLException e) {
            showError("Error loading club statistics", e.getMessage());
        }
    }

    private void loadClubMissionTypesChart(GamificationStatisticsService.ClubStatistics stats) {
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        for (Map.Entry<GoalTypeEnum, Integer> entry : stats.getCompletedMissionsByType().entrySet()) {
            pieChartData.add(new PieChart.Data(entry.getKey().toString(), entry.getValue()));
        }

        clubMissionTypesChart.setData(pieChartData);
        clubMissionTypesChart.setTitle("Completed Missions by Type");
    }

    private void loadClubPerformanceChart(int clubId) {
        try {
            List<Map<String, Object>> performance = statisticsService.getClubPerformanceOverTime(clubId, 5);
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Points Earned");

            for (Map<String, Object> seasonPerformance : performance) {
                String seasonName = (String) seasonPerformance.get("seasonName");
                Integer pointsEarned = (Integer) seasonPerformance.get("pointsEarned");
                series.getData().add(new XYChart.Data<>(seasonName, pointsEarned));
            }

            clubPerformanceChart.getData().clear();
            clubPerformanceChart.getData().add(series);
            clubPerformanceChart.setTitle("Performance Over Time");

        } catch (SQLException e) {
            showError("Error loading club performance chart", e.getMessage());
        }
    }

    private void loadSeasonStatistics(Saison season) {
        try {
            GamificationStatisticsService.SeasonStatistics stats = statisticsService.getSeasonStatistics(season.getId());

            if (stats != null) {
                seasonTotalCompetitionsText.setText(String.valueOf(stats.getTotalCompetitions()));
                seasonActiveCompetitionsText.setText(String.valueOf(stats.getActiveCompetitions()));
                seasonCompletedCompetitionsText.setText(String.valueOf(stats.getCompletedCompetitions()));
                seasonTotalPointsText.setText(String.valueOf(stats.getTotalPointsDistributed()));

                // Load top clubs chart
                loadSeasonTopClubsChart(stats);

                // Load competition types chart
                loadSeasonCompetitionTypesChart(stats);
            }
        } catch (SQLException e) {
            showError("Error loading season statistics", e.getMessage());
        }
    }

    private void loadSeasonTopClubsChart(GamificationStatisticsService.SeasonStatistics stats) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Points");

        for (Map.Entry<String, Integer> entry : stats.getTopClubs().entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        seasonTopClubsChart.getData().clear();
        seasonTopClubsChart.getData().add(series);
        seasonTopClubsChart.setTitle("Top Clubs");
    }

    private void loadSeasonCompetitionTypesChart(GamificationStatisticsService.SeasonStatistics stats) {
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        for (Map.Entry<GoalTypeEnum, Integer> entry : stats.getCompetitionsByType().entrySet()) {
            pieChartData.add(new PieChart.Data(entry.getKey().toString(), entry.getValue()));
        }

        seasonCompetitionTypesChart.setData(pieChartData);
        seasonCompetitionTypesChart.setTitle("Competitions by Type");
    }

    private void loadCompetitionStatistics(Competition competition) {
        try {
            GamificationStatisticsService.CompetitionStatistics stats = statisticsService.getCompetitionStatistics(competition.getId());

            if (stats != null) {
                competitionParticipatingClubsText.setText(String.valueOf(stats.getParticipatingClubs()));
                competitionCompletedByClubsText.setText(String.valueOf(stats.getCompletedByClubs()));
                competitionAverageProgressText.setText(String.format("%.1f%%", stats.getAverageProgress()));
                competitionCompletionRateText.setText(String.format("%.1f%%", stats.getCompletionRate()));

                // Load progress chart
                loadCompetitionProgressChart(stats);
            }
        } catch (SQLException e) {
            showError("Error loading competition statistics", e.getMessage());
        }
    }

    private void loadCompetitionProgressChart(GamificationStatisticsService.CompetitionStatistics stats) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Progress");

        for (GamificationStatisticsService.CompetitionStatistics.ClubProgress clubProgress : stats.getClubProgressList()) {
            series.getData().add(new XYChart.Data<>(clubProgress.getClubName(), clubProgress.getProgress()));
        }

        competitionProgressChart.getData().clear();
        competitionProgressChart.getData().add(series);
        competitionProgressChart.setTitle("Club Progress");
    }

    private void loadLeaderboard() {
        try {
            List<Club> topClubs = statisticsService.getLeaderboard(10);
            ObservableList<Club> leaderboardData = FXCollections.observableArrayList(topClubs);
            leaderboardTable.setItems(leaderboardData);
        } catch (SQLException e) {
            showError("Error loading leaderboard", e.getMessage());
        }
    }

    private void loadActiveMissions() {
        try {
            Map<String, Object> activeMissionsSummary = statisticsService.getActiveMissionsSummary();

            // Load total active missions
            Integer totalActive = (Integer) activeMissionsSummary.get("totalActiveMissions");
            totalActiveMissionsText.setText(String.valueOf(totalActive));

            // Load total points available
            Integer totalPoints = (Integer) activeMissionsSummary.get("totalPointsAvailable");
            totalPointsAvailableText.setText(String.valueOf(totalPoints));

            // Load active missions list
            List<Competition> activeCompetitions = competitionService.getActiveCompetitions();
            ObservableList<Competition> activeMissionsData = FXCollections.observableArrayList(activeCompetitions);
            activeMissionsListView.setItems(activeMissionsData);

            // Load mission type distribution
            @SuppressWarnings("unchecked")
            Map<GoalTypeEnum, Integer> missionsByType = (Map<GoalTypeEnum, Integer>) activeMissionsSummary.get("missionsByType");
            loadMissionTypeDistributionChart(missionsByType);

        } catch (SQLException e) {
            showError("Error loading active missions", e.getMessage());
        }
    }

    private void loadMissionTypeDistributionChart(Map<GoalTypeEnum, Integer> missionsByType) {
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        for (Map.Entry<GoalTypeEnum, Integer> entry : missionsByType.entrySet()) {
            pieChartData.add(new PieChart.Data(entry.getKey().toString(), entry.getValue()));
        }

        missionTypeDistributionChart.setData(pieChartData);
        missionTypeDistributionChart.setTitle("Mission Type Distribution");
    }

    private void loadCompletionTrends() {
        try {
            Map<GoalTypeEnum, Double> trends = statisticsService.getCompetitionCompletionTrends();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Completion Rate");

            for (Map.Entry<GoalTypeEnum, Double> entry : trends.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey().toString(), entry.getValue()));
            }

            completionTrendsChart.getData().clear();
            completionTrendsChart.getData().add(series);
            completionTrendsChart.setTitle("Completion Trends by Goal Type");

        } catch (SQLException e) {
            showError("Error loading completion trends", e.getMessage());
        }
    }

    @FXML
    private void handleRefreshAll() {
        try {
            loadOverviewStatistics();
            loadLeaderboard();
            loadActiveMissions();
            loadCompletionTrends();

            // Refresh selected items if any
            Club selectedClub = clubComboBox.getValue();
            if (selectedClub != null) {
                loadClubStatistics(selectedClub);
            }

            Saison selectedSeason = seasonComboBox.getValue();
            if (selectedSeason != null) {
                loadSeasonStatistics(selectedSeason);
            }

            Competition selectedCompetition = competitionComboBox.getValue();
            if (selectedCompetition != null) {
                loadCompetitionStatistics(selectedCompetition);
            }

            showInfo("Data refreshed successfully");
        } catch (Exception e) {
            showError("Error refreshing data", e.getMessage());
        }
    }

    @FXML
    private void handleExportPDF() {
        // TODO: Implement PDF export functionality
        showInfo("PDF export functionality coming soon!");
    }

    @FXML
    private void handleExportCSV() {
        // TODO: Implement CSV export functionality
        showInfo("CSV export functionality coming soon!");
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInfo(String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    // Add these navigation methods
    @FXML
    public void showUserManagement() {
        try {
            navigateTo("/com/esprit/views/adminDashboard.fxml");
        } catch (IOException e) {
            showError("Navigation Error", "Could not navigate to User Management: " + e.getMessage());
        }
    }

    @FXML
    public void showClubManagement() {
        try {
            navigateTo("/com/esprit/views/adminClubs.fxml");
        } catch (IOException e) {
            showError("Navigation Error", "Could not navigate to Club Management: " + e.getMessage());
        }
    }

    @FXML
    public void showEventManagement() {
        try {
            navigateTo("/com/esprit/views/adminEvents.fxml");
        } catch (IOException e) {
            showError("Navigation Error", "Could not navigate to Event Management: " + e.getMessage());
        }
    }

    @FXML
    public void showProductOrders() {
        try {
            navigateTo("/com/esprit/views/adminProducts.fxml");
        } catch (IOException e) {
            showError("Navigation Error", "Could not navigate to Products & Orders: " + e.getMessage());
        }
    }

    @FXML
    public void showCompetition() {
        try {
            navigateTo("/com/esprit/views/adminCompetition.fxml");
        } catch (IOException e) {
            showError("Navigation Error", "Could not navigate to Competition & Season: " + e.getMessage());
        }
    }

    @FXML
    public void showSurvey() {
        try {
            navigateTo("/com/esprit/views/adminSurvey.fxml");
        } catch (IOException e) {
            showError("Navigation Error", "Could not navigate to Survey Management: " + e.getMessage());
        }
    }

    @FXML
    public void navigateToProfile() {
        try {
            navigateTo("/com/esprit/views/adminProfile.fxml");
        } catch (IOException e) {
            showError("Navigation Error", "Could not navigate to Profile: " + e.getMessage());
        }
    }

    @FXML
    public void handleLogout() {
        // Add logout logic here
        try {
            navigateTo("/com/esprit/views/login.fxml");
        } catch (IOException e) {
            showError("Logout Error", "Could not logout: " + e.getMessage());
        }
    }

    private void navigateTo(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();
        Scene scene = leaderboardTable.getScene(); // Use any existing node to get the scene
        if (scene != null) {
            scene.setRoot(root);
        }
    }
}