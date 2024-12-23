import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;

/**
 * The GradeBook class manages the grades for multiple classes, handling categories, grades,
 * grading scales, rounding options, and calculations for current and hypothetical grades.
 */
public class GradeBook {
    private Scanner scanner = new Scanner(System.in);
    /// Holds classes and their categories and grades for categories
    private HashMap<String, HashMap<String, ArrayList<Double>>> classes = new HashMap<>();
    /// Holds cutoffs for letter grades in each class
    private HashMap<String, ArrayList<Double>> gradingScale = new HashMap<>();
    /// Holds how many items from each category in a class will be dropped
    private HashMap<String, HashMap<String, Double>> drop = new HashMap<>();
    /// Holds whether the class uses rounding or not
    private HashMap<String, Boolean> rounding = new HashMap<>();
    /// Holds what percentage each category is worth
    private HashMap<String, HashMap<String, Double>> percentage = new HashMap<>();
    private static final String[] GRADE_LABELS = {"A+", "A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D+", "D", "D-", "F"};


    private static final String GRADES = "grades.csv";
    private static final String GRADING_SCALE = "gradingScale.csv";
    private static final String DROP = "drop.csv";
    private static final String ROUNDING = "rounding.csv";
    private static final String PERCENTAGE = "percentage.csv";
    private static final double ROUND_SIZE = 0.05;

    public static void main(String[] args) {
        GradeBook gradeBook = new GradeBook();
        boolean loadedSuccessfully = gradeBook.load();
        if (loadedSuccessfully) {
            System.out.println("Grades loaded successfully.");
            gradeBook.displayClassesGrades(); // Optionally display loaded data
        }
        else {
            System.out.println("Failed to load grades. Please check file existence and format.");
        }
    }

    /**
     * Loads all grade book data from files.
     * @return true if all files loaded successfully; false if any fail to load.
     */
    public boolean load() {
        if (!loadPercentage()) {
            System.out.println("Could not load percentages or percentages do not exist");
            return false;
        }
        if (!loadGrades()) {
            System.out.println("Could not load grades or grades do not exist");
            return false;
        }
        initializeClassesAndCategories(); // Move this after loading grades
        if (!loadGradingScale()) {
            System.out.println("Could not load grading scales or grading scales do not exist");
            return false;
        }
        if (!loadRounding()) {
            System.out.println("Could not load rounding or rounding does not exist");
            return false;
        }
        if (!loadDropped()) {
            System.out.println("Could not load dropped or dropped does not exist");
            return false;
        }
        return true;
    }
    /**
     * Initializes the classes and categories based on the loaded percentages.
     * Ensures that each class has its categories initialized in the 'classes' map.
     */
    private void initializeClassesAndCategories() {
        for (String className : percentage.keySet()) {
            HashMap<String, Double> classCategories = percentage.get(className);
            // Ensure 'classes' hashmap has an entry for this class
            classes.computeIfAbsent(className, k -> new HashMap<>());
            HashMap<String, ArrayList<Double>> classGrades = classes.get(className);
            for (String category : classCategories.keySet()) {
                // Ensure 'classes' hashmap has an entry for this category
                classGrades.computeIfAbsent(category, k -> new ArrayList<>());
            }
        }
    }
    private boolean loadGrades() {
        try (BufferedReader reader = new BufferedReader(new FileReader(GRADES))) {
            String header = reader.readLine();  // Read header line
            if (header == null) {
                // File is empty
                return true;
            }
            String line;
            int lineNumber = 1; // Start after the header
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                try {
                    String[] values = line.split(",");
                    if (values.length < 3) {
                        // Skip lines with insufficient data
                        continue;
                    }
                    String className = values[0].trim();
                    String category = values[1].trim();
                    double grade = Double.parseDouble(values[2].trim());

                    // Ensure the class and category entries exist, then add grade
                    classes.computeIfAbsent(className, k -> new HashMap<>())
                            .computeIfAbsent(category, k -> new ArrayList<>())
                            .add(grade);
                } catch (NumberFormatException e) {
                    // Skip invalid number formats
                    continue;
                }
            }
            return true;
        } catch (FileNotFoundException e) {
            // File not found, treat as empty data
            return true;
        } catch (IOException e) {
            System.out.println("An error occurred while loading grades: " + e.getMessage());
            return false;
        }
    }
    private boolean loadGradingScale() {
        try (BufferedReader reader = new BufferedReader(new FileReader(GRADING_SCALE))) {
            String header = reader.readLine();  // Read header line
            if (header == null) {
                // File is empty
                return true;
            }
            String line;
            int lineNumber = 1; // Starting after the header
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                try {
                    String[] values = line.split(",");
                    if (values.length < 2) {
                        // Skip lines with insufficient data
                        continue;
                    }
                    String className = values[0].trim();
                    ArrayList<Double> scale = new ArrayList<>();

                    // Populate the grading scale list for this class
                    for (int i = 1; i < values.length; i++) {
                        String value = values[i].trim();
                        if (!value.isEmpty() && !value.equalsIgnoreCase("null")) {
                            scale.add(Double.parseDouble(value));
                        } else {
                            scale.add(null); // Add null to maintain the scale size
                        }
                    }

                    // Ensure the last cutoff (for "F") is set to 0
                    if (scale.size() < GRADE_LABELS.length) {
                        // Pad with nulls if necessary
                        while (scale.size() < GRADE_LABELS.length) {
                            scale.add(null);
                        }
                    }
                    scale.set(scale.size() - 1, 0.0); // Set "F" cutoff to 0.0

                    gradingScale.put(className, scale);
                } catch (NumberFormatException e) {
                    // Skip invalid number formats
                    continue;
                }
            }
            return true;
        } catch (FileNotFoundException e) {
            // File not found, treat as empty data
            return true;
        } catch (IOException e) {
            System.out.println("An error occurred while loading grading scales: " + e.getMessage());
            return false;
        }
    }

    private boolean loadPercentage() {
        try (BufferedReader reader = new BufferedReader(new FileReader(PERCENTAGE))) {
            reader.readLine();  // Skip header line
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                try {
                    String[] values = line.split(",");
                    if (values.length < 3) {
                        throw new IllegalArgumentException("Not enough values on line " + lineNumber);
                    }
                    String className = values[0].trim();
                    String category = values[1].trim();
                    double percent = Double.parseDouble(values[2].trim());

                    // Ensure class entry exists in percentage map
                    percentage.computeIfAbsent(className, k -> new HashMap<>());

                    // Add category percentage
                    addPercentage(className, category, percent);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid number format on line " + lineNumber + ": " + e.getMessage());
                } catch (IllegalArgumentException e) {
                    System.out.println("Data error on line " + lineNumber + ": " + e.getMessage());
                }
            }
            return true;
        } catch (IOException e) {
            System.out.println("An error occurred while loading percentages: " + e.getMessage());
            return false;
        }
    }
    private boolean loadRounding() {
        try (BufferedReader reader = new BufferedReader(new FileReader(ROUNDING))) {
            reader.readLine();  // Skip header line
            String line;
            int lineNumber = 1; // Start counting after the header
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                try {
                    String[] values = line.split(",");
                    if (values.length < 2) {
                        System.out.println("Data error on line " + lineNumber + ": Not enough values on line " + lineNumber);
                        continue; // Skip to the next line
                    }
                    String className = values[0].trim();
                    boolean round = Boolean.parseBoolean(values[1].trim());
                    rounding.put(className, round);
                } catch (Exception e) {
                    System.out.println("Error processing line " + lineNumber + ": " + e.getMessage());
                }
            }
            return true;
        } catch (IOException e) {
            System.out.println("An error occurred while loading rounding: " + e.getMessage());
            return false;
        }
    }
    private boolean loadDropped() {
        try (BufferedReader reader = new BufferedReader(new FileReader(DROP))) {
            reader.readLine();  // Skip header line
            String line;
            int lineNumber = 1; // Start counting after the header
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                try {
                    String[] values = line.split(",");
                    if (values.length < 3) {
                        System.out.println("Data error on line " + lineNumber + ": Not enough values.");
                        continue; // Skip to the next line
                    }
                    String className = values[0].trim();
                    String category = values[1].trim();
                    double dropped = Double.parseDouble(values[2].trim());
                    drop.computeIfAbsent(className, k -> new HashMap<>()).put(category, dropped);
                } catch (Exception e) {
                    System.out.println("Error processing line " + lineNumber + ": " + e.getMessage());
                }
            }
            return true;
        } catch (IOException e) {
            System.out.println("An error occurred while loading dropped items: " + e.getMessage());
            return false;
        }
    }


    /**
     * Saves all grade book data to files.
     * @return true if all files are saved successfully; false if any fail to save.
     */
    public boolean save() {
        if(!saveGrades()){
            return false;
        }
        if(!saveGradingScale()){
            return false;
        }
        if(!savePercentage()){
            return false;
        }
        if(!saveRounding()){
            return false;
        }
        if(!saveDropped()){
            return false;
        }
        return true;
    }
    private boolean saveGrades() {
        try (FileWriter writer = new FileWriter(GRADES)) {
            // Write the header
            writer.write("Class,Category,Grade\n");

            // Iterate through classes and categories
            for (String className : classes.keySet()) {
                HashMap<String, ArrayList<Double>> categories = classes.get(className);

                for (String category : categories.keySet()) {
                    ArrayList<Double> grades = categories.get(category);

                    // Write each grade in a separate line
                    for (Double grade : grades) {
                        writer.write(className + "," + category + "," + grade + "\n");
                    }
                }
            }
            return true;  // Indicate success
        } catch (IOException e) {
            System.out.println("An error occurred while saving grades: " + e.getMessage());
            return false;  // Indicate failure
        }
    }
    private boolean saveGradingScale() {
        try (FileWriter writer = new FileWriter(GRADING_SCALE)) {
            // Write the header
            writer.write("Class," + String.join(",", GRADE_LABELS) + "\n");

            // Iterate through each class in gradingScale
            for (String className : gradingScale.keySet()) {
                ArrayList<Double> scale = gradingScale.get(className);
                StringBuilder line = new StringBuilder(className);

                // Ensure we have enough cutoffs in `scale` to match `GRADE_LABELS`
                for (int i = 0; i < GRADE_LABELS.length; i++) {
                    if (i < scale.size()) {
                        if (i == GRADE_LABELS.length - 1) {
                            // For "F", ensure the cutoff is 0.0
                            line.append(",").append(0.0);
                        } else {
                            line.append(",").append(scale.get(i));
                        }
                    } else {
                        line.append(",");  // Leave blank if no cutoff for this grade
                    }
                }

                // Write the class and grading scale to the file
                writer.write(line.toString() + "\n");
            }
            return true;
        } catch (IOException e) {
            System.out.println("An error occurred while saving grading scales: " + e.getMessage());
            return false;
        }
    }
    private boolean savePercentage() {
        try (FileWriter writer = new FileWriter(PERCENTAGE)) {
            // Write the header
            writer.write("Class,Category,Percent\n");

            // Iterate through classes and categories
            for (String className : percentage.keySet()) {
                HashMap<String, Double> categories = percentage.get(className);

                for (String category : categories.keySet()) {
                    Double percent = categories.get(category);
                    // Handle null percent by replacing it with an empty value or "NA"
                    writer.write(className + "," + category + "," + (percent != null ? percent : "NA") + "\n");
                }
            }
            return true;  // Indicate success
        } catch (IOException e) {
            System.out.println("An error occurred while saving percentages: " + e.getMessage());
            return false;  // Indicate failure
        }
    }
    private boolean saveRounding() {
        try (FileWriter writer = new FileWriter(ROUNDING)) {
            // Write the header
            writer.write("Class,Round\n");

            // Iterate through classes and write rounding values
            for (String className : rounding.keySet()) {
                boolean round = rounding.get(className);
                writer.write(className + "," + round + "\n");
            }
            return true;  // Indicate success
        } catch (IOException e) {
            System.out.println("An error occurred while saving rounding: " + e.getMessage());
            return false;  // Indicate failure
        }
    }
    private boolean saveDropped() {
        try (FileWriter writer = new FileWriter(DROP)) {
            // Write the header
            writer.write("Class,Category,Dropped\n");

            // Iterate through classes and categories
            for (String className : drop.keySet()) {
                HashMap<String, Double> categories = drop.get(className);

                for (String category : categories.keySet()) {
                    Double dropped = categories.get(category);
                    writer.write(className + "," + category + "," + (dropped != null ? dropped : "0") + "\n");
                }
            }
            return true;  // Indicate success
        } catch (IOException e) {
            System.out.println("An error occurred while saving dropped items: " + e.getMessage());
            return false;
        }
    }

    /**
     * Adds a new class to the grade book.
     * @param className The name of the class to add.
     * @return true if the class is successfully added; false if the class already exists.
     */
    private boolean addClass(String className) {
        if (classes.containsKey(className)) {
            return false;  // Class already exists
        } else {
            classes.put(className, new HashMap<String, ArrayList<Double>>());
        }
        addCategory(className);
        addGradingScale(className);
        System.out.println("Does this class use rounding? (Y/N)");
        String yn = scanner.nextLine().toLowerCase();
        if (yn.equals("y") || yn.equals("yes")) {
            addRounding(className);
        }

        return true;
    }
    /**
     * Prompts the user to add a new class by name.
     * @return The name of the class if it is successfully added; null if the class already exists.
     */
    public String addClass() {
        String className = confirmInput("What class would you like to add?", "Class Name");
        if (addClass(className)) {
            return className;
        } else {
            System.out.println(className + " is already added");
        }
        return null;
    }

    /**
    * Adds a new category to the specified class.
    *
    * @param className The name of the class to add the category to.
    * @param categoryName The name of the category to add.
    * @return true if the category is successfully added, false if the category already exists.
    */
    private boolean addCategory(String className, String categoryName) {
        HashMap<String, ArrayList<Double>> classCategories = classes.get(className);

        if (classCategories.containsKey(categoryName)) {
            return false;  // Category already exists
        }

        classCategories.put(categoryName, new ArrayList<Double>());
        return true;
    }
    /**
     * Prompts the user to add new categories to the specified class.
     * @param className The name of the class to add categories to.
     * @return true if categories are successfully added; false otherwise.
     */
    public boolean addCategory(String className) {
        while (true) {
            String categoryName = confirmInput("What category would you like to add?", "Category Name");
            if (addCategory(className, categoryName)) {
                addPercentage(className, categoryName);
                addDroppedInCategory(className, categoryName);
                System.out.println("Would you like to enter another category? (Y/N)");
                String yn = scanner.nextLine().toLowerCase();
                if (!yn.equals("y") && !yn.equals("yes")) {
                    return true;
                }
            } else {
                System.out.println("Category already exists or entry canceled.");
                // Optionally, ask if the user wants to try adding a different category
                System.out.println("Would you like to try adding a different category? (Y/N)");
                String yn = scanner.nextLine().toLowerCase();
                if (!yn.equals("y") && !yn.equals("yes")) {
                    return false;
                }
            }
        }
    }


    /**
     * Adds a grade to a specific category in a specific class.
     * @param className The name of the class.
     * @param categoryName The name of the category within the class.
     * @param grade The grade to add to the category.
     * @return true if the grade is successfully added.
     */
    private boolean addGrade(String className, String categoryName, double grade) {
        classes.get(className).get(categoryName).add(grade);
        return true;
    }
    /**
     * Prompts the user to add grades to a specified class and category.
     * @return true if the grades are successfully added; false if the grade entry is canceled.
     */
    public boolean addGrade() {
        String className = confirmClassExists("What class would you like to add a grade to?");
        String categoryName = confirmCategoryExists(className, "What category would you like to add a grade to?");
        double grade = getValidGrade();

        if (confirmGrade(grade)) {
            addGrade(className, categoryName, grade);
            System.out.println("Would you like to enter another grade? (Y/N)");
            String yn = scanner.nextLine().toLowerCase();
            if (yn.equals("y") || yn.equals("yes")) {
                addGrade(className, categoryName);
            } else {
                return true;
            }
        } else {
            System.out.println("Grade entry canceled.");
        }

        return false;
    }
    /**
     * Prompts the user to add grades to the specified class and category.
     * @param className The name of the class.
     * @param categoryName The name of the category within the class.
     * @return true if the grades are successfully added; false if the grade entry is canceled.
     */
    public boolean addGrade(String className, String categoryName) {
        double grade = getValidGrade();
        if (confirmGrade(grade)) {
            addGrade(className, categoryName, grade);
            System.out.println("Would you like to enter another grade? (Y/N)");
            String yn = scanner.nextLine().toLowerCase();
            if (yn.equals("y") || yn.equals("yes")) {
                addGrade(className, categoryName);
            } else {
                return true;
            }
        } else {
            System.out.println("Grade entry canceled.");
        }

        return false;
    }

    /**
     * Prompts the user to enter a valid numerical grade.
     * Allows grades over 100% but asks for confirmation if so.
     * @return A valid grade as a double.
     */
    private double getValidGrade() {
        while (true) {
            try {
                System.out.println("Enter the grade (0 or higher; extra credit allowed):");
                String input = scanner.nextLine().trim();
                double grade = Double.parseDouble(input);

                if (grade < 0) {
                    System.out.println("Grade cannot be negative. Please try again.");
                } else if (grade > 100) {
                    // Ask for confirmation if grade is over 100%
                    System.out.printf("You entered %.2f%%, which is over 100%%. Is this correct? (Y/N)\n", grade);
                    String confirmation = scanner.nextLine().trim().toLowerCase();
                    if (confirmation.equals("y") || confirmation.equals("yes")) {
                        return grade;
                    } else {
                        System.out.println("Please re-enter the grade.");
                    }
                } else {
                    return grade;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a numerical grade.");
            }
        }
    }
    /**
     * Confirms the grade entered by the user.
     * @param grade The grade to confirm.
     * @return true if the user confirms the grade; false otherwise.
     */
    private boolean confirmGrade(double grade) {
        System.out.println("Is " + grade + " correct? (Y/N)");
        String confirmation = scanner.nextLine().toLowerCase().trim();
        return confirmation.equals("y") || confirmation.equals("yes");
    }

    /**
     * Adds a grading scale for a specific class.
     * @param className The name of the class to add the grading scale to.
     * @param scale The list of cutoff values for each grade level.
     * @return true if the grading scale is added successfully.
     */
    private boolean addGradingScale(String className, ArrayList<Double> scale) {
        // Ensure the last element corresponds to "F" and is set to 0.0
        if (scale.size() < GRADE_LABELS.length) {
            // Pad with nulls if necessary
            while (scale.size() < GRADE_LABELS.length - 1) {
                scale.add(null);
            }
            scale.add(0.0); // Set "F" cutoff to 0.0
        } else {
            scale.set(GRADE_LABELS.length - 1, 0.0);
        }
        gradingScale.put(className, scale);
        return true;
    }
    /**
     * Prompts the user to add a grading scale for the specified class.
     * @param className The name of the class.
     * @return true if the grading scale is added successfully.
     */
    public boolean addGradingScale(String className) {
        ArrayList<Double> scale = new ArrayList<>();

        // Prompt for each grade and add cutoff or null, skip "F"
        for (int i = 0; i < GRADE_LABELS.length - 1; i++) {
            String grade = GRADE_LABELS[i];
            addCutoff(grade, scale);
        }

        // Automatically set "F" cutoff to 0.0
        scale.add(0.0);

        return addGradingScale(className, scale);
    }
    /**
     * Prompts the user to enter the cutoff for a specific grade and adds it to the grading scale.
     * @param grade The grade label (e.g., "A+", "B").
     * @param scale The list to add the cutoff value to.
     */
    private void addCutoff(String grade, ArrayList<Double> scale) {
        if (grade.equals("F")) {
            // Automatically set "F" cutoff to 0.0 without prompting
            scale.add(0.0);
            return;
        }
        while (true) {
            System.out.println("What is the lower end cutoff for " + grade + "? (if it doesn’t exist, enter 'NA')");
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("na")) {
                scale.add(null);  // Add null for missing grade cutoff
                break;
            } else {
                try {
                    double cutoff = Double.parseDouble(input);
                    scale.add(cutoff);
                    break;
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Please enter a numerical value or 'NA'.");
                }
            }
        }
    }


    /**
     * Adds the number of items to drop in a specific category of a specific class.
     * @param className The name of the class.
     * @param categoryName The name of the category within the class.
     * @param numItems The number of items to drop in the category.
     * @return true if the drop information is successfully added.
     */
    private boolean addDroppedInCategory(String className, String categoryName, double numItems) {
        drop.computeIfAbsent(className, k -> new HashMap<>()).put(categoryName, numItems);
        return true;
    }
    /**
     * Prompts the user to set the number of dropped items for the specified class and category.
     * @param className The name of the class.
     * @param categoryName The name of the category within the class.
     * @return true if the drop information is successfully added.
     */
    public boolean addDroppedInCategory(String className, String categoryName) {
        System.out.println("How many items in " + categoryName + " are dropped?");
        double dropped = getValidPositiveDouble();
        addDroppedInCategory(className, categoryName, dropped);
        return true;
    }


    /**
     * Enables rounding for a specific class.
     * @param className The name of the class.
     * @return true if rounding is successfully enabled; false if rounding is already enabled.
     */
    private boolean addRounding(String className) {
        // If the class doesn't exist in rounding, initialize it
        rounding.putIfAbsent(className, false);
        if (rounding.get(className)) {
            System.out.println("Rounding entry canceled as class already uses rounding.");
            return false;  // Class already has rounding enabled
        }
        rounding.put(className, true);
        return true;
    }
    /**
     * Prompts the user to enable rounding for a specified class.
     * @return true if rounding is successfully enabled; false if rounding is already enabled.
     */
    public boolean addRounding() {
        String className = confirmClassExists("What class would you like to add rounding to?");
        return addRounding(className);
    }


    /**
     * Adds a percentage value for a category within a specific class.
     * @param className The name of the class.
     * @param category The name of the category.
     * @param percent The percentage value for the category.
     * @return true if the percentage is successfully added.
     */
    private boolean addPercentage(String className, String category, double percent) {
        HashMap<String, Double> classCategories = percentage.get(className);
        // Update the percentage for the category
        classCategories.put(category, percent);
        return true;
    }
    /**
     * Prompts the user to add percentages to categories within a specified class.
     * @param className The name of the class.
     * @param categoryName The name of the category.
     * @return true if the percentage is successfully added.
     */
    public boolean addPercentage(String className, String categoryName) {
        Set<String> cats = classes.get(className).keySet();
        ArrayList<String> classCategories = new ArrayList<>(cats);
        // Ensure each category has a place in percentage map for this class
        addPercentageCategories(className, classCategories);
            System.out.println("What is the percentage for " + categoryName + "?");
            double percent = getValidPositiveDouble();
            addPercentage(className, categoryName, percent);
        return true;
    }
    /**
     * Ensures all categories have a percentage placeholder in the specified class.
     * @param className The name of the class.
     * @param categories The list of category names to initialize in the percentage map.
     * @return true if the percentage placeholders are successfully added.
     */
    private boolean addPercentageCategories(String className, ArrayList<String> categories) {
        // Initialize class categories in the percentage map if absent
        percentage.putIfAbsent(className, new HashMap<>());

        HashMap<String, Double> classCategories = percentage.get(className);
        for (String categoryName : categories) {
            classCategories.putIfAbsent(categoryName, null);  // Placeholder for future updates
        }
        return true;
    }


    /**
     * Prompts the user to enter a valid positive double value.
     * @return A valid positive double value.
     */
    private double getValidPositiveDouble() {
        while (true) {
            try {
                double value = Double.parseDouble(scanner.nextLine().trim());
                if (value >= 0) {
                    return value;
                } else {
                    System.out.println("Value must be a positive number. Please try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a numerical value.");
            }
        }
    }
    /**
     * Prompts the user to confirm a text input and allows for reentry until confirmed.
     * @param message The message to prompt the user.
     * @param itemName The name of the item being confirmed.
     * @return The confirmed input string.
     */
    private String confirmInput(String message, String itemName) {
        System.out.println(message);
        String item = scanner.nextLine();
        System.out.println("Is " + item + " correct? (Y/N)");
        String correct = scanner.nextLine().toLowerCase().trim();
        while (!correct.equals("y") && !correct.equals("yes")) {
            System.out.println("What is the correct " + itemName + "?");
            item = scanner.nextLine();
            System.out.println("Is " + item + " correct? (Y/N)");
            correct = scanner.nextLine().toLowerCase().trim();
        }
        return item;
    }
    /**
     * Checks if a class exists in the grade book and prompts the user until a valid class is entered.
     * @param message The prompt message to display.
     * @return The confirmed class name.
     */
    private String confirmClassExists(String message) {
        System.out.println(message);
        String className = scanner.nextLine().trim();
        while (!classes.containsKey(className)) {
            System.out.println(className + " does not exist yet in the grade book. Please try entering again.");
            className = scanner.nextLine().trim();
        }
        return className;
    }
    /**
     * Checks if a category exists in the specified class and prompts the user until a valid category is entered.
     * @param className The name of the class.
     * @param message The prompt message to display.
     * @return The confirmed category name.
     */
    private String confirmCategoryExists(String className, String message) {
        System.out.println(message);
        String categoryName = scanner.nextLine().trim();
        HashMap<String, ArrayList<Double>> classCategories = classes.get(className);
        while (!classCategories.containsKey(categoryName)) {
            System.out.println(categoryName + " does not exist in the class. Please enter a valid category name:");
            categoryName = scanner.nextLine().trim();
        }
        return categoryName;
    }

    /**
     * Displays the grades for each class and category, along with the calculated final and letter grades.
     * Iterates through all classes and their categories, printing each category's grades and the
     * overall calculated final grade and letter grade for each class.
     * @return true if the display operation completes successfully.
     */
    public boolean displayClassesGrades() {
        // Iterate through classes and categories
        for (String className : classes.keySet()) {
            HashMap<String, ArrayList<Double>> categories = classes.get(className);
            System.out.println(className);
            System.out.println("---------------");
            for (String category : categories.keySet()) {
                ArrayList<Double> grades = categories.get(category);

                // Calculate statistics
                double average = calculateAverage(grades);
                double median = calculateMedian(grades);
                double highest = getHighestGrade(grades);
                double lowest = getLowestGrade(grades);

                // Start building the output line
                StringBuilder output = new StringBuilder();
                output.append(category)
                        .append(": ")
                        .append(String.format("%.2f", calculateCatGrade(className, category)))
                        .append(" | Grades: ");

                // Append grades separated by commas
                for (int i = 0; i < grades.size(); i++) {
                    output.append(String.format("%.2f", grades.get(i)));
                    if (i < grades.size() - 1) {
                        output.append(", ");
                    }
                }
                System.out.println(output.toString());

                // Display statistics
                System.out.printf("Statistics for %s:\n", category);
                System.out.printf(" - Average: %.2f\n", average);
                System.out.printf(" - Median: %.2f\n", median);
                System.out.printf(" - Highest: %.2f\n", highest);
                System.out.printf(" - Lowest: %.2f\n", lowest);
                System.out.println();
            }
            // Display final grade and letter grade
            String[] finalGrade = calculateFinalGrade(className);
            System.out.println(String.format("Final Grade: %.2f   %s", Double.parseDouble(finalGrade[0]), finalGrade[1]));
            System.out.println("---------------\n");
        }
        return true;
    }

    /**
     * Calculates the grade for a specified category within a class, considering dropped grades.
     * @param className The name of the class.
     * @param category The name of the category.
     * @return The average grade for the category after dropping the specified number of lowest grades.
     */
    public double calculateCatGrade(String className, String category) {
        double catGrade = 0;
        int dropping = 0;

        // Retrieve the number of items to drop for this specific category
        HashMap<String, Double> dropped = drop.get(className);
        if (dropped != null && dropped.containsKey(category)) {
            dropping = dropped.get(category).intValue();  // Convert to int for indexing
        }

        // Retrieve grades for the specified category
        ArrayList<Double> grades = classes.get(className).get(category);
        if (grades == null || grades.isEmpty()) {
            return 0.0;  // If no grades, return 0
        }
        // Sort grades in ascending order to drop the lowest ones
        grades.sort(null);
        // Sum grades after skipping the lowest `dropping` grades
        for (int i = dropping; i < grades.size(); i++) {
            catGrade += grades.get(i);
        }
        // Calculate the average for remaining grades
        catGrade /= (grades.size() - dropping);  // Adjust denominator based on dropped grades
        return catGrade;
    }

    /**
     * Calculates the final grade and letter grade for the specified class.
     * If the final numeric grade exceeds the highest cutoff, assigns the highest letter grade.
     * @param className The name of the class.
     * @return An array containing the final grade as a string and the corresponding letter grade.
     */
    public String[] calculateFinalGrade(String className) {
        String[] grade = new String[2];  // Array to store final grade and letter grade
        double finalGrade = 0.0;
        HashMap<String, ArrayList<Double>> classCategories = classes.get(className);
        HashMap<String, Double> percents = percentage.get(className);

        // Calculate the final grade
        for (String category : classCategories.keySet()) {
            double categoryPercentage = percents.get(category) / 100.0; // Convert to decimal
            double catGrade = calculateCatGrade(className, category);
            finalGrade += catGrade * categoryPercentage;
        }

        // Apply rounding if enabled for the class
        if (rounding.getOrDefault(className, false)) {
            finalGrade = applyRounding(finalGrade, className);
        }

        // Store the actual final grade (might be over 100%)
        grade[0] = String.format("%.2f", finalGrade);

        // Determine the letter grade
        grade[1] = getLetterGrade(finalGrade, className);

        return grade;
    }
    /**
     * Applies rounding to the final grade if rounding is enabled for the class.
     * @param finalGrade The calculated final grade before rounding.
     * @param className The name of the class.
     * @return The final grade after applying rounding.
     */
    private double applyRounding(double finalGrade, String className) {
        ArrayList<Double> scale = gradingScale.get(className);
        if (scale != null) {
            for (Double cutoff : scale) {
                if (cutoff != null && finalGrade < cutoff && cutoff - finalGrade <= ROUND_SIZE) {
                    return cutoff;
                }
            }
        }
        return finalGrade;
    }
    /**
     * Determines the letter grade corresponding to the final grade based on the class's grading scale.
     * If the final grade exceeds the highest cutoff, assigns the highest letter grade.
     * @param finalGrade The final numeric grade.
     * @param className The name of the class.
     * @return The letter grade corresponding to the final grade.
     */
    private String getLetterGrade(double finalGrade, String className) {
        ArrayList<Double> scale = gradingScale.get(className);
        if (scale != null) {
            String[] letterGrades = GRADE_LABELS; // Use the defined grade labels
            for (int i = 0; i < scale.size(); i++) {
                Double cutoff = scale.get(i);
                if (cutoff != null && finalGrade >= cutoff) {
                    return letterGrades[i];
                }
            }
            // If the final grade is higher than all cutoffs, return the highest grade
            for (int i = 0; i < scale.size(); i++) {
                if (scale.get(i) != null) {
                    return letterGrades[i];
                }
            }
        }
        return "No Scale";
    }
    /**
     * Allows the user to add hypothetical grades to see how they would affect the final grade.
     * Prompts the user to add hypothetical grades to categories and displays the impact on the final grade.
     */
    public void addPossibleGrades() {
        String className = confirmClassExists("For which class would you like to add possible grades?");
        HashMap<String, ArrayList<Double>> classCategories = classes.get(className);

        // Create a deep copy of the classCategories to work with hypothetical grades
        HashMap<String, ArrayList<Double>> hypotheticalCategories = new HashMap<>();
        for (String category : classCategories.keySet()) {
            ArrayList<Double> gradesCopy = new ArrayList<>(classCategories.get(category));
            hypotheticalCategories.put(category, gradesCopy);
        }

        // Prompt the user to add hypothetical grades
        while (true) {
            System.out.println("In which category?");
            String category = scanner.nextLine().trim();
            if (!hypotheticalCategories.containsKey(category)) {
                System.out.println("Category does not exist. Please enter a valid category.");
                continue;
            }
            double grade = getValidGrade();
            hypotheticalCategories.get(category).add(grade);

            System.out.println("Would you like to enter another hypothetical grade? (Y/N)");
            String yn = scanner.nextLine().toLowerCase();
            if (!yn.equals("y") && !yn.equals("yes")) {
                break;
            }
        }

        // Calculate the original and new category grades
        for (String category : hypotheticalCategories.keySet()) {
            double originalCatGrade = calculateCatGrade(className, category);
            double newCatGrade = calculateCatGradeWithGrades(className, category, hypotheticalCategories.get(category));
            System.out.printf("Original %s: %.2f%%\n", category, originalCatGrade);
            System.out.printf("Hypothetical %s: %.2f%%\n", category, newCatGrade);
            System.out.println(" ");
        }

        // Calculate the original and new final grades
        double originalFinalGrade = calculateFinalGradeValue(className);
        double newFinalGrade = calculateFinalGradeWithHypothetical(className, hypotheticalCategories);
        System.out.printf("Original Grade: %.2f%%\n", originalFinalGrade);
        System.out.printf("New Final Grade: %.2f%%\n", newFinalGrade);
        System.out.println("\n");
    }
    /**
     * Calculates the category grade using a provided list of grades, considering dropped grades.
     * @param className The name of the class.
     * @param category The name of the category.
     * @param grades The list of grades to use in the calculation.
     * @return The average grade for the category after dropping the specified number of lowest grades.
     */
    private double calculateCatGradeWithGrades(String className, String category, ArrayList<Double> grades) {
        double catGrade = 0;
        int dropping = 0;

        // Retrieve the number of items to drop for this specific category
        HashMap<String, Double> dropped = drop.get(className);
        if (dropped != null && dropped.containsKey(category)) {
            dropping = dropped.get(category).intValue();  // Convert to int for indexing
        }

        if (grades == null || grades.isEmpty()) {
            return 0.0;  // If no grades, return 0
        }
        // Sort grades in ascending order to drop the lowest ones
        grades.sort(null);
        // Sum grades after skipping the lowest `dropping` grades
        for (int i = dropping; i < grades.size(); i++) {
            catGrade += grades.get(i);
        }
        // Calculate the average for remaining grades
        catGrade /= (grades.size() - dropping);  // Adjust denominator based on dropped grades
        return catGrade;
    }
    /**
     * Calculates the final numeric grade for the specified class.
     * @param className The name of the class.
     * @return The final grade as a double.
     */
    private double calculateFinalGradeValue(String className) {
        String[] grade = calculateFinalGrade(className);
        return Double.parseDouble(grade[0]);
    }
    /**
     * Calculates the final grade using hypothetical grades for the specified class.
     * @param className The name of the class.
     * @param hypotheticalCategories A map of categories to their hypothetical grades.
     * @return The final grade after applying the hypothetical grades.
     */
    private double calculateFinalGradeWithHypothetical(String className, HashMap<String, ArrayList<Double>> hypotheticalCategories) {
        double finalGrade = 0.0;
        HashMap<String, Double> percents = percentage.get(className);

        // Calculate weighted sum of category grades
        for (String category : hypotheticalCategories.keySet()) {
            double categoryPercentage = percents.get(category) / 100.0; // Convert to decimal
            double catGrade = calculateCatGradeWithGrades(className, category, hypotheticalCategories.get(category));
            finalGrade += catGrade * categoryPercentage;
        }
        // Apply rounding if enabled
        if (rounding.getOrDefault(className, false)) {
            finalGrade = applyRounding(finalGrade, className);
        }
        return finalGrade;
    }

    /**
     * Calculates the grades needed in remaining assignments to achieve a desired letter grade.
     * Prompts the user for the desired letter grade and the number of remaining items in each category,
     * then calculates the required average grades needed per remaining item.
     */
    public void calculateNeededGradesForLetterGrade() {
        String className = confirmClassExists("For which class do you want to calculate needed grades?");
        // Get the desired letter grade
        String desiredLetterGrade = getDesiredLetterGrade(className);

        double desiredFinalPercentage = getPercentageForLetterGrade(className, desiredLetterGrade);
        if (desiredFinalPercentage < 0) {
            System.out.println("Invalid letter grade or grading scale not defined for this class.");
            return;
        }

        HashMap<String, Double> percents = percentage.get(className);
        HashMap<String, ArrayList<Double>> classCategories = classes.get(className);
        if (percents == null || classCategories == null) {
            System.out.println("No categories or percentages defined for this class.");
            return;
        }

        // Calculate the current total contribution towards the final grade
        double currentTotal = 0.0;
        for (String category : percents.keySet()) {
            double categoryWeight = percents.get(category) / 100.0;
            ArrayList<Double> grades = classCategories.get(category);
            if (grades != null && !grades.isEmpty()) {
                double catGrade = calculateCatGrade(className, category);
                currentTotal += catGrade * categoryWeight;
            }
        }

        // Always prompt for the number of remaining assignments in each category
        HashMap<String, Integer> remainingItems = new HashMap<>();
        for (String category : percents.keySet()) {
            int itemsLeft = getRemainingItemsForCategory(category);
            remainingItems.put(category, itemsLeft);
        }

        // Calculate the total weight of categories that still have assignments left
        double remainingWeight = 0.0;
        for (String category : percents.keySet()) {
            double categoryWeight = percents.get(category) / 100.0;
            int itemsLeft = remainingItems.get(category);
            if (itemsLeft > 0) {
                remainingWeight += categoryWeight;
            }
        }

        double requiredRemainingTotal = desiredFinalPercentage - currentTotal;

        // If no remaining assignments but desired is higher than current total, it's impossible
        if (remainingWeight == 0 && requiredRemainingTotal > 0) {
            System.out.println("No remaining assignments to improve your grade. It's not possible to reach " + desiredLetterGrade + ".");
            return;
        }

        if (requiredRemainingTotal <= 0) {
            // Already surpass desired grade
            System.out.printf("You have already met or exceeded the requirements for a %s (%.2f%%). No additional points needed.\n",
                    desiredLetterGrade, desiredFinalPercentage);
            return;
        }

        // Check if reaching the desired grade is possible
        if (requiredRemainingTotal > remainingWeight * 100) {
            System.out.println("It's not possible to achieve the desired final grade with the current grades and remaining assignments.");
            return;
        }

        // Distribute the needed improvement proportionally based on category weights
        for (String category : remainingItems.keySet()) {
            int itemsLeft = remainingItems.get(category);
            if (itemsLeft <= 0) {
                // No future assignments in this category, skip calculations
                continue;
            }

            double categoryWeight = percents.get(category) / 100.0;
            double weightProportion = categoryWeight / remainingWeight;
            double categoryNeededTotal = requiredRemainingTotal * weightProportion;

            // Calculate existing category stats after dropping the lowest grades
            ArrayList<Double> grades = classCategories.get(category);
            double sumExisting = 0.0;
            int existingCount = 0;
            if (grades != null && !grades.isEmpty()) {
                ArrayList<Double> catGrades = new ArrayList<>(grades);
                catGrades.sort(null);
                int dropping = 0;
                if (drop.containsKey(className) && drop.get(className).containsKey(category)) {
                    dropping = drop.get(className).get(category).intValue();
                }
                for (int i = dropping; i < catGrades.size(); i++) {
                    sumExisting += catGrades.get(i);
                    existingCount++;
                }
            }

            double currentCatGrade = (existingCount > 0) ? (sumExisting / existingCount) : 0.0;
            double currentCatContribution = currentCatGrade * categoryWeight;

            // Final category grade needed
            // (currentCatContribution + categoryNeededTotal) = catGradeFinal * categoryWeight
            double catGradeFinal = (currentCatContribution + categoryNeededTotal) / categoryWeight;

            // Solve for needed average on the remaining assignments:
            // catGradeFinal = (sumExisting + neededGrade * itemsLeft) / (existingCount + itemsLeft)
            // neededGrade * itemsLeft = catGradeFinal * (existingCount + itemsLeft) - sumExisting
            double neededGrade = (catGradeFinal * (existingCount + itemsLeft) - sumExisting) / itemsLeft;

            // Print results
            if (neededGrade > 100) {
                System.out.printf("To achieve %s (%.2f%%), in category '%s' you would need an average of %.2f%% on the remaining %d assignments, which is above 100%% and not possible.\n",
                        desiredLetterGrade, desiredFinalPercentage, category, neededGrade, itemsLeft);
            } else if (neededGrade < 0) {
                // If the needed grade is less than 0, it means the requirement is already surpassed
                System.out.printf("For category '%s', you have already secured enough points. No additional points needed to reach %s.\n",
                        category, desiredLetterGrade);
            } else {
                System.out.printf("To achieve %s (%.2f%%) in category '%s', you need an average of %.2f%% on the remaining %d assignments.\n",
                        desiredLetterGrade, desiredFinalPercentage, category, neededGrade, itemsLeft);
            }
        }

        System.out.println();
    }

    /**
     * Prompts the user to enter the number of remaining items in a category.
     * @param category The name of the category.
     * @return The number of remaining items as an integer.
     */
    private int getRemainingItemsForCategory(String category) {
        while (true) {
            try {
                System.out.printf("Enter the number of remaining items in category '%s': ", category);
                int itemsLeft = Integer.parseInt(scanner.nextLine().trim());
                if (itemsLeft >= 0) {
                    return itemsLeft;
                } else {
                    System.out.println("Number of items cannot be negative. Please try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a whole number.");
            }
        }
    }
    /**
     * Prompts the user to enter a desired letter grade and validates it against the grading scale for the specified class.
     * @param className The name of the class.
     * @return The desired letter grade as a string.
     */
    private String getDesiredLetterGrade(String className) {
        // Get the valid letter grades for the class based on the grading scale
        ArrayList<Double> scale = gradingScale.get(className);
        if (scale == null) {
            System.out.println("Grading scale not defined for this class.");
            return null;
        }
        String[] letterGrades = {"A+", "A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D+", "D", "D-", "F"};

        // Create a list of valid letter grades based on non-null cutoffs
        ArrayList<String> validLetterGrades = new ArrayList<>();
        for (int i = 0; i < scale.size(); i++) {
            if (scale.get(i) != null) {
                validLetterGrades.add(letterGrades[i]);
            }
        }

        System.out.println("Enter the desired letter grade (e.g., A, B+, C-):");
        String letterGrade = scanner.nextLine().trim().toUpperCase();
        while (!validLetterGrades.contains(letterGrade)) {
            System.out.println("Invalid letter grade. Valid options are: " + validLetterGrades);
            letterGrade = scanner.nextLine().trim().toUpperCase();
        }
        return letterGrade;
    }
    /**
     * Maps the desired letter grade to its corresponding minimum percentage cutoff.
     * @param className The name of the class.
     * @param letterGrade The desired letter grade.
     * @return The minimum percentage required for the desired letter grade.
     */
    private double getPercentageForLetterGrade(String className, String letterGrade) {
        ArrayList<Double> scale = gradingScale.get(className);
        if (scale == null) {
            return -1;
        }
        String[] letterGrades = {"A+", "A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D+", "D", "D-", "F"};
        int index = -1;
        for (int i = 0; i < letterGrades.length; i++) {
            if (letterGrades[i].equals(letterGrade)) {
                index = i;
                break;
            }
        }
        if (index == -1 || index >= scale.size()) {
            return -1;
        }
        Double cutoff = scale.get(index);
        if (cutoff == null) {
            System.out.println("Cutoff not defined for grade " + letterGrade);
            return -1;
        }
        return cutoff;
    }
    /**
     * Deletes a class and all its associated data.
     * Prompts the user for the class name and confirmation before deletion.
     */
    public void deleteClass() {
        if (classes.isEmpty()) {
            System.out.println("No classes available to delete.");
            return;
        }

        String className = confirmClassExists("Enter the name of the class you want to delete:");
        System.out.println("Are you sure you want to delete the class '" + className + "' and all its data? (Y/N)");
        String confirmation = scanner.nextLine().trim().toLowerCase();
        if (confirmation.equals("y") || confirmation.equals("yes")) {
            // Remove the class from all data structures
            classes.remove(className);
            gradingScale.remove(className);
            drop.remove(className);
            rounding.remove(className);
            percentage.remove(className);

            System.out.println("Class '" + className + "' has been deleted.");
        } else {
            System.out.println("Deletion cancelled.");
        }
    }

    /**
     * Deletes all saved data.
     * Prompts the user for confirmation before deletion.
     */
    public void deleteAllData() {
        System.out.println("Are you sure you want to delete ALL data? This action cannot be undone. (Y/N)");
        String confirmation = scanner.nextLine().trim().toLowerCase();
        if (confirmation.equals("y") || confirmation.equals("yes")) {
            // Clear all data structures
            classes.clear();
            gradingScale.clear();
            drop.clear();
            rounding.clear();
            percentage.clear();

            // Delete all save files
            deleteFile(GRADES);
            deleteFile(GRADING_SCALE);
            deleteFile(DROP);
            deleteFile(ROUNDING);
            deleteFile(PERCENTAGE);

            System.out.println("All data has been deleted.");
        } else {
            System.out.println("Deletion cancelled.");
        }
    }

    /**
     * Deletes a file if it exists.
     *
     * @param fileName The name of the file to delete.
     */
    private void deleteFile(String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            if (!file.delete()) {
                System.out.println("Failed to delete file: " + fileName);
            }
        }
    }

    /**
     * Allows the user to edit or delete existing grades.
     */
    public void editGrade() {
        if (classes.isEmpty()) {
            System.out.println("No classes available. Please add a class first.");
            return;
        }

        String className = confirmClassExists("Enter the class name where you want to edit grades:");
        String categoryName = confirmCategoryExists(className, "Enter the category name where you want to edit grades:");
        ArrayList<Double> grades = classes.get(className).get(categoryName);

        if (grades.isEmpty()) {
            System.out.println("No grades available in this category to edit.");
            return;
        }

        // Display grades with indices
        System.out.println("Grades in " + categoryName + ":");
        for (int i = 0; i < grades.size(); i++) {
            System.out.printf("%d) %.2f\n", i + 1, grades.get(i));
        }

        int index = -1;
        while (true) {
            System.out.println("Enter the number of the grade you want to edit or delete (or 0 to cancel):");
            String input = scanner.nextLine().trim();
            try {
                index = Integer.parseInt(input);
                if (index == 0) {
                    System.out.println("Edit operation cancelled.");
                    return;
                }
                if (index < 1 || index > grades.size()) {
                    System.out.println("Invalid selection. Please try again.");
                    continue;
                }
                break;
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number corresponding to the grade.");
            }
        }

        // Choose to edit or delete
        System.out.println("Do you want to edit or delete this grade? (E/D)");
        String choice = scanner.nextLine().trim().toLowerCase();
        if (choice.equals("e") || choice.equals("edit")) {
            // Edit the grade
            double newGrade = getValidGrade();
            grades.set(index - 1, newGrade);
            System.out.println("Grade updated successfully.");
        } else if (choice.equals("d") || choice.equals("delete")) {
            // Confirm deletion
            System.out.println("Are you sure you want to delete this grade? (Y/N)");
            String confirm = scanner.nextLine().trim().toLowerCase();
            if (confirm.equals("y") || confirm.equals("yes")) {
                grades.remove(index - 1);
                System.out.println("Grade deleted successfully.");
            } else {
                System.out.println("Deletion cancelled.");
            }
        } else {
            System.out.println("Invalid choice. Operation cancelled.");
        }
    }

    /**
     * Calculates the average of a list of grades.
     */
    private double calculateAverage(ArrayList<Double> grades) {
        if (grades == null || grades.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (double grade : grades) {
            sum += grade;
        }
        return sum / grades.size();
    }

    /**
     * Calculates the median of a list of grades.
     */
    private double calculateMedian(ArrayList<Double> grades) {
        if (grades == null || grades.isEmpty()) {
            return 0.0;
        }
        ArrayList<Double> sortedGrades = new ArrayList<>(grades);
        sortedGrades.sort(null);
        int middle = sortedGrades.size() / 2;
        if (sortedGrades.size() % 2 == 0) {
            return (sortedGrades.get(middle - 1) + sortedGrades.get(middle)) / 2.0;
        } else {
            return sortedGrades.get(middle);
        }
    }

    /**
     * Returns the highest grade from a list.
     */
    private double getHighestGrade(ArrayList<Double> grades) {
        if (grades == null || grades.isEmpty()) {
            return 0.0;
        }
        double highest = grades.get(0);
        for (double grade : grades) {
            if (grade > highest) {
                highest = grade;
            }
        }
        return highest;
    }

    /**
     * Returns the lowest grade from a list.
     */
    private double getLowestGrade(ArrayList<Double> grades) {
        if (grades == null || grades.isEmpty()) {
            return 0.0;
        }
        double lowest = grades.get(0);
        for (double grade : grades) {
            if (grade < lowest) {
                lowest = grade;
            }
        }
        return lowest;
    }

    /**
     * Displays help information to assist the user.
     */
    public void displayHelp() {
        System.out.println("GradeBook Application Help");
        System.out.println("==========================");
        System.out.println("This application allows you to manage grades for multiple classes.");
        System.out.println("Here are the available options:");
        System.out.println("1) Add a new class - Create a new class and set up its grading criteria.");
        System.out.println("2) Add new grade(s) - Enter grades for assignments in a class and category.");
        System.out.println("3) View Grades and Statistics - Display all grades and detailed statistics for each class.");
        System.out.println("4) Add possible grades - See how hypothetical grades affect your final grade.");
        System.out.println("5) How to get wanted grade - Calculate what you need to achieve a desired final grade.");
        System.out.println("6) Edit or Delete Grades - Modify or remove existing grades.");
        System.out.println("7) Delete a class - Remove a class and all its data.");
        System.out.println("8) Delete all data - Remove all data from the application.");
        System.out.println("9) Help - Display this help information.");
        System.out.println("10) Exit - Save your data and exit the application.");
        System.out.println("\nFor more detailed instructions, please refer to the user manual.");
        System.out.println("If you have any questions, feel free to contact support.");
        System.out.println();
    }

}