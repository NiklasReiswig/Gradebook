import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Backup {
    Scanner scanner = new Scanner(System.in);
    // Holds classes and their categories and grades for categories
    HashMap<String, HashMap<String, ArrayList<Double>>> classes = new HashMap<>();

    // Holds cutoffs for letter grades in each class
    HashMap<String, ArrayList<Double>> gradingScale = new HashMap<>();

    // Holds how many items from each category in a class will be dropped
    HashMap<String, HashMap<String, ArrayList<Double>>> drop = new HashMap<>();

    // Holds whether the class uses rounding or not
    HashMap<String, Boolean> rounding = new HashMap<>();

    // Holds what percentage each category is worth
    HashMap<String, HashMap<String, Double>> percentage = new HashMap<>();

    private boolean loadGrades() {
        // Load grades logic here
        return false;
    }

    private boolean saveGrades() {
        // Save grades logic here
        return false;
    }

    // Method to add a new class
    private boolean addClass(String className) {
        if (classes.containsKey(className)) {
            return false;  // Class already exists
        } else {
            // Create a new categories HashMap for this class
            classes.put(className, new HashMap<String, ArrayList<Double>>());
        }
        return true;
    }
    public boolean addClass(){
        String className, correct;
        System.out.println("what class would you like to add?");
        className = scanner.nextLine();
        System.out.println("Is " + className + " correct (Y/N)?");
        correct = scanner.nextLine().toLowerCase();  // Standardizing input to lowercase

        // Loop until correct input is confirmed
        while (!correct.equals("y") && !correct.equals("yes")) {  // Use AND condition
            System.out.println("What is the correct entry?");
            className = scanner.nextLine();
            System.out.println("Is " + className + " correct (Y/N)?");
            correct = scanner.nextLine().toLowerCase();  // Standardizing input to lowercase
        }
        if(addClass(className)) {
            return true;
        }
        else{
            System.out.println(className+" is already added");
        }
        return false;
    }

    // Method to add a new category to a specific class
    private boolean addCategory(String className, String categoryName) {
        // Get the class's categories
        HashMap<String, ArrayList<Double>> classCategories = classes.get(className);

        // Check if the category already exists in the class
        if (classCategories.containsKey(categoryName)) {
            return false;  // Category already exists
        }

        // Add a new category with an empty ArrayList for grades
        classCategories.put(categoryName, new ArrayList<Double>());
        return true;
    }
    public boolean addCategory(){
        System.out.println("What class are you adding a category to?");
        String categoryName, correct;
        String className = scanner.nextLine();
        // Check if the class exists in the classes
        while(!classes.containsKey(className)) {
            System.out.println(className+" does not exist yet in the grade book. please try entering again.");
            className = scanner.nextLine();
        }
        System.out.println("what category would you like to add?");
        categoryName = scanner.nextLine();
        System.out.println("Is " + categoryName + " correct (Y/N)?");
        correct = scanner.nextLine().toLowerCase();  // Standardizing input to lowercase

        // Loop until correct input is confirmed
        while (!correct.equals("y") && !correct.equals("yes")) {  // Use AND condition
            System.out.println("What is the correct entry?");
            categoryName = scanner.nextLine();
            System.out.println("Is " + categoryName + " correct (Y/N)?");
            correct = scanner.nextLine().toLowerCase();  // Standardizing input to lowercase
        }
        if(addCategory(className, categoryName)) {
            return true;
        }
        else{
            System.out.println(categoryName+" is already added");
        }
        return false;
    }

    private boolean addGrade(String className, String categoryName, double grade) {
        classes.get(className).get(categoryName).add(grade);
        return true;
    }
    public boolean addGrade() {
        System.out.println("What class would you like to add a grade to?");
        String className = scanner.nextLine().trim();

        // Validate class exists
        while (!classes.containsKey(className)) {
            System.out.println("That class is not yet in the grade book. Please enter a valid class name:");
            className = scanner.nextLine().trim();
        }

        System.out.println("What category would you like to add a grade to?");
        String categoryName = scanner.nextLine().trim();

        // Validate category exists within the class
        HashMap<String, ArrayList<Double>> classCategories = classes.get(className);
        while (!classCategories.containsKey(categoryName)) {
            System.out.println("That category is not yet in the class. Please enter a valid category name:");
            categoryName = scanner.nextLine().trim();
        }

        // Prompt for grade and handle exceptions
        double grade = getValidGrade();

        // Confirm the grade with user before adding
        if (confirmGrade(grade)) {
            addGrade(className, categoryName, grade);
            System.out.println("would you like to enter another grade? (Y/N)");
            String yn = scanner.nextLine().toLowerCase();
            if(yn.equals("y") || yn.equals("yes")) {
                addGrade();
            }
            else{
                return true;
            }
        } else {
            System.out.println("Grade entry canceled.");
        }

        return false;
    }
    // Helper method to safely parse grade input
    private double getValidGrade() {
        while (true) {
            try {
                System.out.println("What grade would you like to add?");
                double grade = Double.parseDouble(scanner.nextLine().trim());

                if (grade < 0) {
                    System.out.println("Grade must be a positive number. Please try again.");
                } else {
                    return grade;  // Return valid grade
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a numerical grade.");
            }
        }
    }
    // Helper method to confirm grade with user, especially for extra credit
    private boolean confirmGrade(double grade) {
        String confirmation;
        if (grade > 100) {
            System.out.println("Are you sure you'd like to add " + (grade - 100) + " points of extra credit? (Y/N)");
        } else {
            System.out.println("Is " + grade + " correct? (Y/N)");
        }

        confirmation = scanner.nextLine().toLowerCase().trim();
        return confirmation.equals("y") || confirmation.equals("yes");
    }

    private boolean addGradingScale(String className, ArrayList<Double> scale) {
        gradingScale.put(className, scale);  // Simply set the scale since we are adding it
        return true;
    }
    public boolean addGradingScale() {
        ArrayList<Double> scale = new ArrayList<>();
        System.out.println("What class would you like to add a grading scale to?");
        String className = scanner.nextLine().trim();

        // Validate class exists
        while (!classes.containsKey(className)) {
            System.out.println("That class is not yet in the grade book. Please enter a valid class name:");
            className = scanner.nextLine().trim();
        }

        // Add each cutoff to the scale
        addCutoff("A+", scale);
        addCutoff("A", scale);
        addCutoff("A-", scale);
        addCutoff("B+", scale);
        addCutoff("B-", scale);
        addCutoff("C+", scale);
        addCutoff("C-", scale);
        addCutoff("D+", scale);
        addCutoff("D", scale);
        addCutoff("D-", scale);
        addCutoff("F", scale);

        // Add grading scale to the class
        return addGradingScale(className, scale);
    }
    // Helper method to add a cutoff to the scale
    private void addCutoff(String grade, ArrayList<Double> scale) {
        System.out.println("What is the lower end cutoff for " + grade + "? (if it doesn’t exist, enter 'NA')");
        String input = scanner.nextLine().trim().toLowerCase();

        if (!input.equals("na")) {
            try {
                double cutoff = Double.parseDouble(input);
                scale.add(cutoff);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a numerical value or 'NA'.");
                addCutoff(grade, scale);  // Retry for the current grade
            }
        }
    }

    private boolean droppedInCategory(String className, String categoryName, double numItems) {
        HashMap<String, ArrayList<Double>> classCategories = drop.get(className);
        classCategories.get(categoryName).add(numItems);
        return true;
    }
    public boolean droppedInCategory() {
        System.out.println("What class would you like to change the amount of dropped items in?");
        String className = scanner.nextLine().trim();

        // Validate class exists
        while (!classes.containsKey(className)) {
            System.out.println("That class is not yet in the grade book. Please enter a valid class name:");
            className = scanner.nextLine().trim();
        }

        System.out.println("What category would you like to change the amount of dropped items in?");
        String categoryName = scanner.nextLine().trim();

        // Validate category exists within the class
        HashMap<String, ArrayList<Double>> classCategories = classes.get(className);
        while (!classCategories.containsKey(categoryName)) {
            System.out.println("That category is not yet in the class. Please enter a valid category name:");
            categoryName = scanner.nextLine().trim();
        }
        System.out.println("How many items in "+categoryName+" are dropped?");
        double dropped = scanner.nextDouble();
        droppedInCategory(className, categoryName, dropped);

        return true;
    }

    private boolean addRounding(String className) {
        if (!rounding.containsKey(className)) {
            return false;
        }
        rounding.put(className, true);
        return true;
    }
    public boolean addRounding() {
        String className = confirmClassExists("what class would you like to add rounding to");
        if (addRounding(className)){
            return true;
        }
        else{
            System.out.println("Rounding entry canceled as class already uses rounding.");
            return false;
        }

    }

    private boolean addPercentage(String className, String category, double percent) {
        HashMap<String, Double> classCategories = percentage.get(className);
        // Update the percentage for the category (replace existing value with new value)
        classCategories.put(category, percent);
        return true;  // Successfully updated
    }
    public boolean addPercentage() {
        String className = confirmClassExists("what class would you like to add percentages to its categories?");
        int numCat = classes.get(className).keySet().size();
        ArrayList<String> categoryNames = new ArrayList<>(classes.get(className).keySet());
        addPercentageCategories(className, categoryNames);
        for (String categoryName : categoryNames) {
            System.out.println("what is the percentage for " + categoryName + "?");
            double percent = getValidPositiveDouble();
            addPercentage(className, categoryName, percent);
        }
        return true;
    }
    private boolean addPercentageCategories(String className, ArrayList<String> categories) {
        HashMap<String, Double> clas = new HashMap<>();
        if (!percentage.containsKey(className)) {
            percentage.put(className, clas); // Class not found
        }
        HashMap<String, Double> classCategories = percentage.get(className);
        for (String categoryName : categories) {
            percentage.get(className).put(categoryName,null);
        }
        return true;
    }
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

    private String confirmClassExists(String message) {
        System.out.println(message);
        String className = scanner.nextLine().trim();
        while (!classes.containsKey(className)) {
            System.out.println(className + " does not exist yet in the grade book. Please try entering again.");
            className = scanner.nextLine().trim();
        }
        return className;
    }

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


}

