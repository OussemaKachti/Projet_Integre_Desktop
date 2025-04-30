
package com.esprit.visualization;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bar chart visualization for comments by month for a specific club
 */
public class CommentBarChartVisualization {
    private final StackPane container;
    private BarChart<String, Number> barChart;
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy");

    /**
     * Create a comment bar chart visualization in the given container
     * 
     * @param container StackPane to put the visualization in
     */
    public CommentBarChartVisualization(StackPane container) {
        this.container = container;
        initialize();
    }

    private void initialize() {
        // Create the x and y axes
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Month");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Number of Comments");

        // Create the bar chart
        barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Comments by Month");
        barChart.setAnimated(true);
        barChart.setLegendVisible(true);

        // Set bar chart to fill container
        barChart.prefWidthProperty().bind(container.widthProperty());
        barChart.prefHeightProperty().bind(container.heightProperty());

        // Style improvements
        barChart.getStyleClass().add("custom-bar-chart");

        // Replace container content with the chart
        container.getChildren().clear();
        container.getChildren().add(barChart);
    }

    /**
     * Update the bar chart with new data for a specific club
     * 
     * @param comments List of comments to visualize
     * @param clubName Name of the selected club
     */
    public void updateData(List<Map<String, Object>> comments, String clubName) {
        // Run on JavaFX thread if not already
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> updateDataInternal(comments, clubName));
        } else {
            updateDataInternal(comments, clubName);
        }
    }

    private void updateDataInternal(List<Map<String, Object>> comments, String clubName) {
        barChart.getData().clear();

        if (comments == null || comments.isEmpty()) {
            Label noDataLabel = new Label("No comments available for selected club");
            noDataLabel.setTextFill(Color.GRAY);
            noDataLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
            container.getChildren().clear();
            container.getChildren().add(noDataLabel);
            return;
        }

        // Set title based on club name
        barChart.setTitle(
                "Comments by Month" + (clubName != null && !clubName.equals("all") ? " for " + clubName : ""));

        // Get current year and month
        LocalDate currentDate = LocalDate.now();

        // Create a Map to count comments by month
        Map<String, Integer> commentsByMonth = new HashMap<>();

        // Initialize the last 12 months with zero counts
        for (int i = 11; i >= 0; i--) {
            YearMonth yearMonth = YearMonth.from(currentDate).minusMonths(i);
            commentsByMonth.put(yearMonth.format(MONTH_FORMATTER), 0);
        }

        // Log comments for debugging
        System.out.println("Processing " + comments.size() + " comments for visualization");

        // Count comments by month
        for (Map<String, Object> comment : comments) {
            String dateStr = (String) comment.get("date");
            if (dateStr != null) {
                try {
                    LocalDate date = LocalDate.parse(dateStr);
                    YearMonth yearMonth = YearMonth.from(date);
                    String monthKey = yearMonth.format(MONTH_FORMATTER);

                    // Only count if it's within the last 12 months
                    if (commentsByMonth.containsKey(monthKey)) {
                        commentsByMonth.put(monthKey, commentsByMonth.get(monthKey) + 1);
                        System.out.println(
                                "Added comment for month: " + monthKey + ", count: " + commentsByMonth.get(monthKey));
                    }
                } catch (Exception e) {
                    System.err.println("Error processing comment date: " + dateStr);
                    e.printStackTrace();
                }
            }
        }

        // Create a series for the chart
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(clubName != null && !clubName.equals("all") ? clubName : "All Clubs");

        // Add data to the series in chronological order
        for (int i = 11; i >= 0; i--) {
            YearMonth yearMonth = YearMonth.from(currentDate).minusMonths(i);
            String monthKey = yearMonth.format(MONTH_FORMATTER);
            int count = commentsByMonth.get(monthKey);
            series.getData().add(new XYChart.Data<>(monthKey, count));
            System.out.println("Added data point: Month=" + monthKey + ", Count=" + count);
        }

        // Add the series to the chart
        barChart.getData().add(series);

        // Define simple colors to use instead of gradients
        Color[] barColors = {
                Color.rgb(94, 114, 228), // #5e72e4 - blue
                Color.rgb(17, 205, 239), // #11cdef - cyan
                Color.rgb(45, 206, 137), // #2dce89 - green
                Color.rgb(251, 99, 64), // #fb6340 - orange
                Color.rgb(245, 54, 92), // #f5365c - red
                Color.rgb(137, 101, 224), // #8965e0 - purple
                Color.rgb(173, 181, 189), // #adb5bd - gray
                Color.rgb(255, 214, 0), // #ffd600 - yellow
                Color.rgb(45, 206, 137), // #2dce89 - green (repeated)
                Color.rgb(94, 114, 228), // #5e72e4 - blue (repeated)
                Color.rgb(17, 205, 239), // #11cdef - cyan (repeated)
                Color.rgb(137, 101, 224) // #8965e0 - purple (repeated)
        };

        // Apply colors directly to the bars via the nodes
        // This happens after JavaFX has created the nodes
        Platform.runLater(() -> {
            if (!series.getData().isEmpty()) {
                // Wait a short time to ensure nodes are created
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Apply colors to the bars
                for (int i = 0; i < series.getData().size(); i++) {
                    XYChart.Data<String, Number> item = series.getData().get(i);

                    if (item.getNode() != null) {
                        // Choose a color for this bar
                        Color color = barColors[i % barColors.length];

                        // Set the color directly on the node background
                        item.getNode().setStyle(
                                "-fx-background-color: rgba(" +
                                        (int) (color.getRed() * 255) + "," +
                                        (int) (color.getGreen() * 255) + "," +
                                        (int) (color.getBlue() * 255) + "," +
                                        color.getOpacity() + ");");

                        // Add a hover effect
                        Color finalColor = color;
                        item.getNode().setOnMouseEntered(e -> {
                            // Darker version on hover
                            Color darkerColor = finalColor.darker();
                            item.getNode().setStyle(
                                    "-fx-background-color: rgba(" +
                                            (int) (darkerColor.getRed() * 255) + "," +
                                            (int) (darkerColor.getGreen() * 255) + "," +
                                            (int) (darkerColor.getBlue() * 255) + "," +
                                            darkerColor.getOpacity() + ");" +
                                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 8, 0, 0, 0);");
                        });

                        item.getNode().setOnMouseExited(e -> {
                            // Reset to original color
                            item.getNode().setStyle(
                                    "-fx-background-color: rgba(" +
                                            (int) (color.getRed() * 255) + "," +
                                            (int) (color.getGreen() * 255) + "," +
                                            (int) (color.getBlue() * 255) + "," +
                                            color.getOpacity() + ");");
                        });

                        // Add tooltip with value
                        Tooltip tooltip = new Tooltip(
                                String.format("%s: %d comments", item.getXValue(), item.getYValue().intValue()));
                        tooltip.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
                        Tooltip.install(item.getNode(), tooltip);
                    } else {
                        System.err.println("No node available for data item at index " + i);
                    }
                }
            } else {
                System.err.println("Series is empty, no data to color");
            }
        });

        // Make sure the bar chart is visible
        if (!container.getChildren().contains(barChart)) {
            container.getChildren().clear();
            container.getChildren().add(barChart);
        }
    }
}
