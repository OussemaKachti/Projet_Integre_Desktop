package com.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;

import com.esprit.models.Evenement;
import com.esprit.services.ServiceEvent;
import com.esprit.utils.DataSource;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

public class CalendarViewController implements Initializable {

    @FXML
    private Label monthYearLabel;

    @FXML
    private Button previousButton;

    @FXML
    private Button nextButton;

    @FXML
    private Button todayButton;

    @FXML
    private GridPane calendarGrid;

    @FXML
    private VBox eventDetailsPane;

    @FXML
    private HBox weekdaysHeader;

    @FXML
    private Button backToListButton;

    @FXML
    private ComboBox<String> categoryFilter;

    @FXML
    private ComboBox<String> clubFilter;

    private YearMonth currentYearMonth;
    private Map<LocalDate, List<Evenement>> eventsByDate;
    private ServiceEvent serviceEvent;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialize the service and current date
        serviceEvent = new ServiceEvent();
        currentYearMonth = YearMonth.now();
        eventsByDate = new HashMap<>();

        // Set up UI components
        setupWeekdaysHeader();
        setupFilters();

        // Set up button actions
        previousButton.setOnAction(e -> previousMonth());
        nextButton.setOnAction(e -> nextMonth());
        todayButton.setOnAction(e -> goToToday());
        backToListButton.setOnAction(e -> backToListView());

        // Add filter change listeners
        categoryFilter.valueProperty().addListener((obs, oldVal, newVal) -> refreshCalendar());
        clubFilter.valueProperty().addListener((obs, oldVal, newVal) -> refreshCalendar());

        // Load all events and populate calendar
        loadEvents();
        populateCalendar();
    }

    private void setupWeekdaysHeader() {
        weekdaysHeader.getChildren().clear();
        weekdaysHeader.setSpacing(5);

        // Create labels for each day of the week
        for (DayOfWeek day : DayOfWeek.values()) {
            Label dayLabel = new Label(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()));
            dayLabel.setPrefWidth(100);
            dayLabel.setAlignment(Pos.CENTER);
            dayLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
            weekdaysHeader.getChildren().add(dayLabel);
        }
    }

    private void setupFilters() {
        // Set up category filter
        categoryFilter.getItems().add("All Categories");
        categoryFilter.getItems().addAll(serviceEvent.getAllCategoriesNames());
        categoryFilter.setValue("All Categories");

        // Set up club filter
        clubFilter.getItems().add("All Clubs");
        clubFilter.getItems().addAll(serviceEvent.getAllClubsNames());
        clubFilter.setValue("All Clubs");
    }

    private void loadEvents() {
        try {
            Connection conn = DataSource.getInstance().getCnx();
            String categoryCondition = "";
            String clubCondition = "";

            // Apply category filter if selected
            if (categoryFilter.getValue() != null && !categoryFilter.getValue().equals("All Categories")) {
                int categoryId = serviceEvent.getCategorieIdByName(categoryFilter.getValue());
                categoryCondition = " AND categorie_id = " + categoryId;
            }

            // Apply club filter if selected
            if (clubFilter.getValue() != null && !clubFilter.getValue().equals("All Clubs")) {
                int clubId = serviceEvent.getClubIdByName(clubFilter.getValue());
                clubCondition = " AND club_id = " + clubId;
            }

            String query = "SELECT * FROM evenement WHERE 1=1" + categoryCondition + clubCondition + " ORDER BY start_date";
            PreparedStatement pst = conn.prepareStatement(query);
            ResultSet rs = pst.executeQuery();

            // Clear existing events
            eventsByDate.clear();

            while (rs.next()) {
                Evenement event = new Evenement();
                event.setId(rs.getInt("id"));
                event.setNom_event(rs.getString("nom_event"));
                event.setType(rs.getString("type"));
                event.setDesc_event(rs.getString("desc_event"));
                event.setImage_description(rs.getString("image_description"));
                event.setLieux(rs.getString("lieux"));
                event.setClub_id(rs.getInt("club_id"));
                event.setCategorie_id(rs.getInt("categorie_id"));
                event.setStart_date(rs.getDate("start_date"));
                event.setEnd_date(rs.getDate("end_date"));

                // Convert java.sql.Date to LocalDate
                // Convert java.sql.Date to LocalDate
                LocalDate startDate = event.getStart_date() != null ?
                        LocalDate.parse(event.getStart_date().toString()) : null;
                LocalDate endDate = event.getEnd_date() != null ?
                        LocalDate.parse(event.getEnd_date().toString()) : null;

                // Add event to all dates in its range
                LocalDate date = startDate;
                while (!date.isAfter(endDate)) {
                    if (!eventsByDate.containsKey(date)) {
                        eventsByDate.put(date, new ArrayList<>());
                    }
                    eventsByDate.get(date).add(event);
                    date = date.plusDays(1);
                }
            }

        } catch (SQLException ex) {
            System.err.println("Error loading events for calendar: " + ex.getMessage());
        }
    }

    private void populateCalendar() {
        // Update the month and year label
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
        monthYearLabel.setText(currentYearMonth.format(formatter));

        // Clear the calendar grid
        calendarGrid.getChildren().clear();

        // Determine the date for cell (0, 0)
        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7; // Adjust for Sunday as the first day (0)

        // Fill the calendar with day cells
        LocalDate date = firstOfMonth.minusDays(dayOfWeek);
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 7; col++) {
                VBox dayCell = createDayCell(date);
                calendarGrid.add(dayCell, col, row);
                date = date.plusDays(1);
            }
        }
    }

    private VBox createDayCell(LocalDate date) {
        VBox dayCell = new VBox(5);
        dayCell.setAlignment(Pos.TOP_CENTER);
        dayCell.setPrefHeight(100);
        dayCell.setPrefWidth(100);
        dayCell.setStyle("-fx-border-color: #ddd; -fx-padding: 5;");

        // Create date label
        Label dateLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dateLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        // Style differently if the date is outside current month
        if (!date.getMonth().equals(currentYearMonth.getMonth())) {
            dayCell.setStyle("-fx-border-color: #ddd; -fx-padding: 5; -fx-background-color: #f8f8f8;");
            dateLabel.setTextFill(Color.LIGHTGRAY);
        }

        // Highlight today's date
        if (date.equals(LocalDate.now())) {
            dateLabel.setStyle("-fx-background-color: #1e90ff; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 10;");
        }

        dayCell.getChildren().add(dateLabel);

        // Add events for this date
        if (eventsByDate.containsKey(date)) {
            List<Evenement> events = eventsByDate.get(date);
            for (Evenement event : events.stream().limit(3).collect(Collectors.toList())) {
                HBox eventIndicator = createEventIndicator(event);
                dayCell.getChildren().add(eventIndicator);
            }

            // Show count if there are more events
            if (events.size() > 3) {
                Label moreLabel = new Label("+" + (events.size() - 3) + " more");
                moreLabel.setStyle("-fx-text-fill: #666;");
                moreLabel.setFont(Font.font("System", 10));
                dayCell.getChildren().add(moreLabel);
            }

            // Make the cell clickable to show all events for that day
            dayCell.setOnMouseClicked(e -> showEventsForDay(date, events));
        }

        return dayCell;
    }

    private HBox createEventIndicator(Evenement event) {
        HBox indicator = new HBox();
        indicator.setAlignment(Pos.CENTER_LEFT);
        indicator.setPrefWidth(90);
        indicator.setMaxWidth(90);

        // Choose color based on event type
        String backgroundColor;
        switch (event.getType().toLowerCase()) {
            case "open":
                backgroundColor = "#040f71"; // Blue for open events
                break;
            case "closed":
                backgroundColor = "#dc3545"; // Red for closed events
                break;
            default:
                backgroundColor = "#6c757d"; // Grey for other types
                break;
        }

        // Create colored dot
        Region dot = new Region();
        dot.setMinSize(8, 8);
        dot.setMaxSize(8, 8);
        dot.setStyle("-fx-background-color: " + backgroundColor + "; -fx-background-radius: 4;");

        // Create event name label
        Label nameLabel = new Label(event.getNom_event());
        nameLabel.setMaxWidth(80);
        nameLabel.setStyle("-fx-text-fill: #333; -fx-font-size: 10px;");
        nameLabel.setEllipsisString("...");

        // Add elements to indicator
        indicator.getChildren().addAll(dot, nameLabel);
        HBox.setMargin(nameLabel, new javafx.geometry.Insets(0, 0, 0, 5));

        // Make the indicator clickable to show event details
        indicator.setOnMouseClicked(e -> {
            showEventDetails(event);
            e.consume(); // Prevent the click from reaching the day cell
        });

        return indicator;
    }

    private void showEventsForDay(LocalDate date, List<Evenement> events) {
        // Clear the details pane
        eventDetailsPane.getChildren().clear();

        // Create header
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
        Label dateHeader = new Label(date.format(formatter));
        dateHeader.setFont(Font.font("System", FontWeight.BOLD, 16));
        dateHeader.setStyle("-fx-padding: 0 0 10 0;");

        // Create scrollable list of events
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox eventsBox = new VBox(10);
        eventsBox.setStyle("-fx-padding: 10;");

        // Add events to the list
        for (Evenement event : events) {
            eventsBox.getChildren().add(createEventCard(event));
        }

        scrollPane.setContent(eventsBox);

        // Add everything to the details pane
        eventDetailsPane.getChildren().addAll(dateHeader, scrollPane);
    }

    private void showEventDetails(Evenement event) {
        // Clear the details pane
        eventDetailsPane.getChildren().clear();

        // Create back button to return to day view
        Button backButton = new Button("« Back to Day View");
        backButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #1e90ff;");
        backButton.setOnAction(e -> {
            LocalDate eventDate = event.getStart_date().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            if (eventsByDate.containsKey(eventDate)) {
                showEventsForDay(eventDate, eventsByDate.get(eventDate));
            }
        });

        // Create event title
        Label titleLabel = new Label(event.getNom_event());
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        titleLabel.setStyle("-fx-padding: 10 0;");

        // Create details container
        VBox detailsBox = new VBox(10);
        detailsBox.setStyle("-fx-padding: 10;");

        // Event type badge
        HBox typeBox = new HBox();
        Label typeLabel = new Label(event.getType());
        typeLabel.setStyle("-fx-padding: 3 8; -fx-background-radius: 10; -fx-text-fill: white;");

        if ("Open".equalsIgnoreCase(event.getType())) {
            typeLabel.setStyle(typeLabel.getStyle() + "-fx-background-color: #040f71;");
        } else if ("Closed".equalsIgnoreCase(event.getType())) {
            typeLabel.setStyle(typeLabel.getStyle() + "-fx-background-color: #dc3545;");
        } else {
            typeLabel.setStyle(typeLabel.getStyle() + "-fx-background-color: #6c757d;");
        }

        typeBox.getChildren().add(typeLabel);

        // Event category and club
        String categoryName = serviceEvent.getCategoryNameById(event.getCategorie_id());
        String clubName = serviceEvent.getClubNameById(event.getClub_id());

        Label categoryClubLabel = new Label(categoryName + " • " + clubName);
        categoryClubLabel.setStyle("-fx-text-fill: #666;");

        // Event dates
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");
        String dateRange;
        if (dateFormat.format(event.getStart_date()).equals(dateFormat.format(event.getEnd_date()))) {
            dateRange = dateFormat.format(event.getStart_date());
        } else {
            dateRange = dateFormat.format(event.getStart_date()) + " - " + dateFormat.format(event.getEnd_date());
        }

        Label dateLabel = new Label("📅 " + dateRange);

        // Event location
        Label locationLabel = new Label("📍 " + event.getLieux());

        // Event description
        Label descTitleLabel = new Label("Description:");
        descTitleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        descTitleLabel.setStyle("-fx-padding: 10 0 5 0;");

        TextArea descriptionArea = new TextArea(event.getDesc_event());
        descriptionArea.setWrapText(true);
        descriptionArea.setEditable(false);
        descriptionArea.setPrefHeight(100);
        descriptionArea.setStyle("-fx-control-inner-background: #f8f8f8;");

        // View details button
        Button viewDetailsButton = new Button("View Full Details");
        viewDetailsButton.setStyle("-fx-background-color: #1e90ff; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 15;");
        viewDetailsButton.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/DetailsEvent.fxml"));
                Parent root = loader.load();

                // Get the controller and set the event data
                DetailsEvent detailsController = loader.getController();
                detailsController.setEventData(event);

                viewDetailsButton.getScene().setRoot(root);

            } catch (IOException ex) {
                System.err.println("Error loading DetailsEvent.fxml: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        // Add everything to the details box
        detailsBox.getChildren().addAll(
                typeBox,
                categoryClubLabel,
                dateLabel,
                locationLabel,
                descTitleLabel,
                descriptionArea,
                viewDetailsButton
        );

        // Add scroll capability for longer content
        ScrollPane scrollPane = new ScrollPane(detailsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        // Add everything to the details pane
        eventDetailsPane.getChildren().addAll(backButton, titleLabel, scrollPane);
    }

    private VBox createEventCard(Evenement event) {
        // Create card container
        VBox card = new VBox(5);
        card.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-background-radius: 5; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 1);");

        // Event title
        Label titleLabel = new Label(event.getNom_event());
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        // Event type badge
        Label typeLabel = new Label(event.getType());
        typeLabel.setStyle("-fx-padding: 2 6; -fx-background-radius: 10; -fx-text-fill: white; -fx-font-size: 10px;");

        if ("Open".equalsIgnoreCase(event.getType())) {
            typeLabel.setStyle(typeLabel.getStyle() + "-fx-background-color: #040f71;");
        } else if ("Closed".equalsIgnoreCase(event.getType())) {
            typeLabel.setStyle(typeLabel.getStyle() + "-fx-background-color: #dc3545;");
        } else {
            typeLabel.setStyle(typeLabel.getStyle() + "-fx-background-color: #6c757d;");
        }

        // Event category and club
        String categoryName = serviceEvent.getCategoryNameById(event.getCategorie_id());
        String clubName = serviceEvent.getClubNameById(event.getClub_id());

        Label categoryClubLabel = new Label(categoryName + " • " + clubName);
        categoryClubLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        // Event location
        Label locationLabel = new Label("📍 " + event.getLieux());
        locationLabel.setStyle("-fx-font-size: 11px;");

        // Container for title and type
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setSpacing(10);
        headerBox.getChildren().addAll(titleLabel, typeLabel);

        // Add everything to the card
        card.getChildren().addAll(headerBox, categoryClubLabel, locationLabel);

        // Make the card clickable to show event details
        card.setOnMouseClicked(e -> showEventDetails(event));

        return card;
    }

    private void previousMonth() {
        currentYearMonth = currentYearMonth.minusMonths(1);
        populateCalendar();
    }

    private void nextMonth() {
        currentYearMonth = currentYearMonth.plusMonths(1);
        populateCalendar();
    }

    private void goToToday() {
        currentYearMonth = YearMonth.now();
        populateCalendar();

        // Check if there are events today and show them
        LocalDate today = LocalDate.now();
        if (eventsByDate.containsKey(today)) {
            showEventsForDay(today, eventsByDate.get(today));
        } else {
            // Clear details if no events today
            eventDetailsPane.getChildren().clear();
            Label noEventsLabel = new Label("No events scheduled for today.");
            noEventsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
            eventDetailsPane.getChildren().add(noEventsLabel);
        }
    }

    private void backToListView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/AfficherEvent.fxml"));
            Parent root = loader.load();
            backToListButton.getScene().setRoot(root);
        } catch (IOException ex) {
            System.err.println("Error loading AfficherEvent.fxml: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void refreshCalendar() {
        loadEvents();
        populateCalendar();
        eventDetailsPane.getChildren().clear();
    }
}