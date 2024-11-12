import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;

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
    /// Could Holds grades for each category
    private HashMap<String, HashMap<String, Double>> catGrades = new HashMap<>();
    /// Could Holds final grade for each class
    private HashMap<String, String[]> finalGrades = new HashMap<>();


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
     * Loads all gradeBook data from files.
     * @return true if all files loaded successfully; false if any fail to load.
     */
    public boolean load() {
        if (!loadPercentage()) {
            System.out.println("Could not load percentages or percentages do not exist");
            return false;
        }
        initializeClassesAndCategories();
        if (!loadGrades()) {
            System.out.println("Could not load grades or grades do not exist");
            return false;
        }
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
     * Saves all gradeBook data to files.
     * @return true if all files saved successfully; false if any fail to save.
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
            writer.write("Class,A+,A,A-,B+,B,B-,C+,C,C-,D+,D,D-,F\n");

            // Grade labels in order for reference
            String[] gradeLabels = {"A+", "A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D+", "D", "D-", "F"};

            // Iterate through each class in gradingScale
            for (String className : gradingScale.keySet()) {
                ArrayList<Double> scale = gradingScale.get(className);
                StringBuilder line = new StringBuilder(className);

                // Ensure we have enough cutoffs in `scale` to match `gradeLabels`
                for (int i = 0; i < gradeLabels.length; i++) {
                    if (i < scale.size()) {
                        line.append(",").append(scale.get(i));
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
     * Adds a new class to the gradeBook.
     *
     * @param className The name of the class to add.
     * @return true if the class is successfully added, false if the class already exists.
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
     *
     * @return true if the class is successfully added, false if the class already exists.
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
     * Prompts the user to add a new category to a specified class.
     *
     * @return true if the category is successfully added, false if the category already exists.
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
     *
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
     * Prompts the user to add a grade to a specified class and category.
     *
     * @return true if the grade is successfully added, false if the grade entry is canceled.
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
     *
     * @return A valid grade as a double.
     */
    private double getValidGrade() {
        while (true) {
            try {
                System.out.println("What grade would you like to add?");
                double grade = Double.parseDouble(scanner.nextLine().trim());

                if (grade < 0) {
                    System.out.println("Grade must be a positive number. Please try again.");
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
     *
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
     *
     * @param className The name of the class to add the grading scale to.
     * @param scale The list of cutoff values for each grade level.
     * @return true if the grading scale is added successfully.
     */
    private boolean addGradingScale(String className, ArrayList<Double> scale) {
        gradingScale.put(className, scale);
        return true;
    }
    /**
     * Prompts the user to add a grading scale for a specified class.
     *
     * @return true if the grading scale is added successfully.
     */
    public boolean addGradingScale(String className) {
        ArrayList<Double> scale = new ArrayList<>();

        // Standard grade labels
        String[] grades = {"A+", "A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D+", "D", "D-", "F"};

        // Prompt for each grade and add cutoff or null
        for (String grade : grades) {
            addCutoff(grade, scale);
        }

        return addGradingScale(className, scale);
    }
    /**
     * Prompts the user to enter the cutoff for a specific grade and adds it to the grading scale.
     *
     * @param grade The grade label (e.g., "A+", "B").
     * @param scale The list to add the cutoff value to.
     */
    private void addCutoff(String grade, ArrayList<Double> scale) {
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
     * Adds the number of dropped items in a specific category of a specific class.
     *
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
     * Prompts the user to set the number of dropped items for a specified class and category.
     *
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
     *
     * @param className The name of the class.
     * @return true if rounding is successfully enabled, false if rounding is already enabled.
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
     *
     * @return true if rounding is successfully enabled, false if rounding is already enabled.
     */
    public boolean addRounding() {
        String className = confirmClassExists("What class would you like to add rounding to?");
        return addRounding(className);
    }


    /**
     * Adds a percentage value for a category within a specific class.
     *
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
     *
     * @return true if all percentages are successfully added.
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
     *
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
     *
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
     *
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
     * Checks if a class exists in the gradeBook and prompts the user until a valid class is entered.
     *
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
     *
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
     *
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
     * @param className the name of the class.
     * @param category the name of the category.
     * @return the average grade for the category after dropping the specified number of lowest grades.
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

    public String[] calculateFinalGrade(String className) {
        String[] grade = new String[2];  // Array to store final grade and letter grade
        double finalGrade = 0.0;
        // Get the categories and their percentages for the specified class
        HashMap<String, ArrayList<Double>> classCategories = classes.get(className);
        HashMap<String, Double> percents = percentage.get(className);
        // Calculate weighted sum of category grades
        for (String category : classCategories.keySet()) {
            double categoryPercentage = percents.get(category) / 100.0; // Convert to decimal
            finalGrade += calculateCatGrade(className, category) * categoryPercentage;
        }
        // Apply rounding if enabled for the class
        if (rounding.getOrDefault(className, false)) {
            finalGrade = applyRounding(finalGrade, className);
        }
        grade[0] = String.format("%.2f", finalGrade);  // Store the rounded final grade
        // Find the letter grade by matching finalGrade to the grading scale
        grade[1] = getLetterGrade(finalGrade, className);
        return grade;
    }
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
    private String getLetterGrade(double finalGrade, String className) {
        ArrayList<Double> scale = gradingScale.get(className);
        if (scale != null) {
            String[] letterGrades = {"A+", "A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D+", "D", "D-", "F"};
            for (int i = 0; i < scale.size(); i++) {
                Double cutoff = scale.get(i);
                if (cutoff != null && finalGrade >= cutoff) {
                    return letterGrades[i];
                }
            }
        }
        return "No Scale";
    }

}