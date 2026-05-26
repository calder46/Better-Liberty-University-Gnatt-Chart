package Dashboard;

import JavaFinalProject.Course;
import JavaFinalProject.Ganttural_Resolution;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controls the main dashboard UI.
 * Handles course loading, drag-and-drop, save, and load functionality.
 * 
 * @author Jacob Wingate
 */
public class DashboardController {

    @FXML private TextField loadURL;
    @FXML private Button loadButton;

    // Semester and storage columns
    @FXML private VBox remainingCourses, externalCourses;
    @FXML private VBox year1Fall, year1Spring, year2Fall, year2Spring;
    @FXML private VBox year3Fall, year3Spring, year4Fall, year4Spring;

    // Total credit labels
    @FXML private Label remainingTotal, externalTotal;
    @FXML private Label year1FallTotal, year1SpringTotal, year2FallTotal, year2SpringTotal;
    @FXML private Label year3FallTotal, year3SpringTotal, year4FallTotal, year4SpringTotal;

    // Maps each column VBox to its credit total label
    private Map<VBox, Label> totalMap = new HashMap<>();

    // Maps column name string to its VBox for save/load lookup
    private Map<String, VBox> columnLookup = new HashMap<>();

    @FXML
    public void initialize() {
        // Map columns to their total labels
        totalMap.put(remainingCourses, remainingTotal);
        totalMap.put(externalCourses, externalTotal);
        totalMap.put(year1Fall, year1FallTotal);
        totalMap.put(year1Spring, year1SpringTotal);
        totalMap.put(year2Fall, year2FallTotal);
        totalMap.put(year2Spring, year2SpringTotal);
        totalMap.put(year3Fall, year3FallTotal);
        totalMap.put(year3Spring, year3SpringTotal);
        totalMap.put(year4Fall, year4FallTotal);
        totalMap.put(year4Spring, year4SpringTotal);

        // Map column name strings to VBoxes for save/load
        columnLookup.put("remainingCourses", remainingCourses);
        columnLookup.put("externalCourses", externalCourses);
        columnLookup.put("year1Fall", year1Fall);
        columnLookup.put("year1Spring", year1Spring);
        columnLookup.put("year2Fall", year2Fall);
        columnLookup.put("year2Spring", year2Spring);
        columnLookup.put("year3Fall", year3Fall);
        columnLookup.put("year3Spring", year3Spring);
        columnLookup.put("year4Fall", year4Fall);
        columnLookup.put("year4Spring", year4Spring);

        VBox[] semesterBoxes = {
            remainingCourses, externalCourses,
            year1Fall, year1Spring, year2Fall, year2Spring,
            year3Fall, year3Spring, year4Fall, year4Spring
        };

        for (VBox box : semesterBoxes) {
            if (box != null) setupDropTarget(box);
        }
        updateAllTotals();
    }

    /**
     * Handles the Load URL button click.
     * Validates the URL is from catalog.liberty.edu before scraping.
     */
    @FXML
    private void handleLoadAction() {
        String url = loadURL.getText();
        if (url == null || url.trim().isEmpty()) {
            System.out.println("Error: URL field is empty.");
            return;
        }

        // Validate URL is from Liberty's catalog before scraping
        if (!url.contains("catalog.liberty.edu")) {
            System.out.println("Error: URL must be from catalog.liberty.edu");
            return;
        }

        Ganttural_Resolution.runScraper(url);

        // Clear existing courses before importing new ones
        remainingCourses.getChildren().clear();
        externalCourses.getChildren().clear();

        importData();
        updateAllTotals();
    }

    /**
     * Pulls courses from the scraper and populates the UI columns.
     */
    public void importData() {
        ArrayList<Course> importedCourses = Ganttural_Resolution.courses;
        if (importedCourses == null || importedCourses.isEmpty()) return;

        for (Course c : importedCourses) {
            if (c.getHours() == 0) {
                addCourseToColumn(externalCourses, c.getName(), "0");
            } else {
                addCourseToColumn(remainingCourses, c.getName(), String.valueOf(c.getHours()));
            }
        }
    }

    /**
     * Handles the Save button click.
     * Collects the current layout and serializes it to a user-chosen file.
     */
    @FXML
    private void handleSaveAction() {
        // Build layout map: column name -> list of [courseName, credits] pairs
        Map<String, List<String[]>> layout = new HashMap<>();

        for (Map.Entry<String, VBox> entry : columnLookup.entrySet()) {
            String columnName = entry.getKey();
            VBox box = entry.getValue();
            List<String[]> courses = new ArrayList<>();

            for (javafx.scene.Node node : box.getChildren()) {
                if (node instanceof StackPane) {
                    Label nameLabel = (Label) node.lookup("#courseLabel");
                    Label creditsLabel = (Label) node.lookup("#courseCredits");
                    if (nameLabel != null && creditsLabel != null) {
                        courses.add(new String[]{nameLabel.getText(), creditsLabel.getText()});
                    }
                }
            }
            layout.put(columnName, courses);
        }

        // Open file chooser for save destination
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Layout");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("DAT Files", "*.dat")
        );
        File file = fileChooser.showSaveDialog(new Stage());
        if (file != null) {
            SaveData.save(new SaveData(layout), file);
        }
    }

    /**
     * Handles the Load button click (right panel).
     * Deserializes a saved layout and restores courses to their columns.
     */
    @FXML
    private void handleLoadFileAction() {
        // Open file chooser to pick a saved layout
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Layout");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("DAT Files", "*.dat")
        );
        File file = fileChooser.showOpenDialog(new Stage());
        if (file == null) return;

        SaveData data = SaveData.load(file);
        if (data == null) return;

        // Clear all columns before restoring
        for (VBox box : columnLookup.values()) {
            if (box != null) box.getChildren().clear();
        }

        // Restore courses to their saved columns
        for (Map.Entry<String, List<String[]>> entry : data.getLayout().entrySet()) {
            VBox column = columnLookup.get(entry.getKey());
            if (column == null) continue;
            for (String[] course : entry.getValue()) {
                addCourseToColumn(column, course[0], course[1]);
            }
        }
        updateAllTotals();
    }

    private void addCourseToColumn(VBox column, String name, String credits) {
        try {
            FXMLLoader loader = new FXMLLoader(DashboardController.class.getResource("/Dashboard/CourseBlock.fxml"));
            Parent courseNode = loader.load();

            CourseBlockController controller = loader.getController();
            controller.setCourseDetails(name, credits);

            applyLevelColor(courseNode, name);

            if (column != null) column.getChildren().add(courseNode);
        } catch (IOException e) {
            System.err.println("Error: Could not load CourseBlock.fxml - " + e.getMessage());
        }
    }

    private void applyLevelColor(Parent node, String courseName) {
        String baseStyle = "-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #808080; -fx-border-width: 1;";
        String bgColor = "-fx-background-color: #D3D3D3;";

        Pattern pattern = Pattern.compile("\\d");
        Matcher matcher = pattern.matcher(courseName);

        if (matcher.find()) {
            char firstDigit = matcher.group().charAt(0);
            switch (firstDigit) {
                case '1': bgColor = "-fx-background-color: #CCFFFF;"; break;
                case '2': bgColor = "-fx-background-color: #CCFFCC;"; break;
                case '3': bgColor = "-fx-background-color: #FFFFCC;"; break;
                case '4': bgColor = "-fx-background-color: #FFCCCC;"; break;
            }
        }
        node.setStyle(bgColor + baseStyle);
    }

    private void setupDropTarget(VBox target) {
        target.setOnDragOver(event -> {
            if (event.getGestureSource() != target && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        target.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasString()) {
                StackPane source = (StackPane) event.getGestureSource();
                VBox oldParent = (VBox) source.getParent();
                if (oldParent != null) {
                    oldParent.getChildren().remove(source);
                    target.getChildren().add(source);
                    updateAllTotals();
                }
            }
            event.setDropCompleted(true);
            event.consume();
        });
    }

    private void updateAllTotals() {
        totalMap.forEach((box, label) -> {
            if (box != null && label != null) updateTotalCredits(box, label);
        });
    }

    private void updateTotalCredits(VBox semesterBox, Label totalLabel) {
        int total = 0;
        for (javafx.scene.Node node : semesterBox.getChildren()) {
            if (node instanceof StackPane) {
                Label creditsLabel = (Label) node.lookup("#courseCredits");
                if (creditsLabel != null) {
                    try {
                        total += Integer.parseInt(creditsLabel.getText());
                    } catch (NumberFormatException e) { }
                }
            }
        }
        totalLabel.setText("Total Credits: " + total);
    }
}
