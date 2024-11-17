import java.util.Scanner;

public class Driver {
    private static final Scanner scanner = new Scanner(System.in);
    private static Thread shutdownHook;  // Store the shutdown hook reference

    public static void main(String[] args) {
        GradeBook gradeBook = new GradeBook();
        boolean loadedSuccessfully = gradeBook.load();

        if (!loadedSuccessfully) {
            System.out.println("Some data files were not found or could not be loaded. Starting with empty data.");
        } else {
            System.out.println("Grades loaded successfully.");
            gradeBook.displayClassesGrades();
        }

        // Add a shutdown hook to save on unexpected exits
        shutdownHook = new Thread(() -> {
            System.out.println("Autosaving data before exit...");
            if (gradeBook.save()) {
                System.out.println("Data saved successfully.");
            } else {
                System.out.println("Data not saved.");
            }
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        // Start the main program loop
        runProgramLoop(gradeBook);
    }

    /**
     * Runs the main program loop, prompting the user for actions.
     * Exits and prompts to save when the user chooses to exit.
     *
     * @param gradeBook the GradeBook instance to interact with
     */
    private static void runProgramLoop(GradeBook gradeBook) {
        while (true) {
            System.out.println("Choose an option:\n"
                    + "1) Add a new class\n"
                    + "2) Add new grade(s)\n"
                    + "3) View Grades\n"
                    + "4) Add possible grades\n"
                    + "5) How to get wanted grade\n"
                    + "6) Delete a class\n"
                    + "7) Delete all data\n"
                    + "8) Exit\n");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    gradeBook.addClass();
                    break;
                case "2":
                    gradeBook.addGrade();
                    break;
                case "3":
                    gradeBook.displayClassesGrades();
                    break;
                case "4":
                    gradeBook.addPossibleGrades();
                    break;
                case "5":
                    gradeBook.calculateNeededGradesForLetterGrade();
                    break;
                case "6":
                    gradeBook.deleteClass();
                    break;
                case "7":
                    gradeBook.deleteAllData();
                    break;
                case "8":
                    exitWithPrompt(gradeBook); // Ask before exiting
                    return; // Exit loop if user confirms
                default:
                    System.out.println("Invalid choice. Please try again.");
                    break;
            }
        }
    }

    /**
     * Prompts the user to save before exiting the program.
     *
     * @param gradeBook the GradeBook instance to save data for
     */
    private static void exitWithPrompt(GradeBook gradeBook) {
        System.out.println("Do you want to save before exiting? (Y/N)");
        String choice = scanner.nextLine().toLowerCase();
        if (choice.equals("y") || choice.equals("yes")) {
            gradeBook.save();
        } else {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException e) {
                // Ignore if the shutdown is already in progress
            }
        }
        System.out.println("Exiting program...");
    }
}