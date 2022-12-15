package org.example;

import org.example.model.TestClass;

import java.io.File;
import java.io.IOException;


public class App {
    public static void main( String[] args ) throws IOException {
        /*firstTaskTest();*/
        secondTaskTest();

    }

    public static void firstTaskTest() throws IOException {
        String finesFolder = "fines";
        String statisticsOfFines = "fines\\statistics\\statistics.xml";
        int numberOfTreads = 4;
        //Check README.md to understand the time results of using different numbers of threads
        ParallelStatistics.parallelParsingStatistics(finesFolder, statisticsOfFines, numberOfTreads);
    }

    public static void secondTaskTest() throws IOException {
        File file = new File("src/main/resources/class.properties");
        TestClass testClass = UtilsWithProperties.loadFromProperties(TestClass.class, file.toPath());
        System.out.println(testClass.getIntProperty());
        System.out.println(testClass.getIntegerProperty());
        System.out.println(testClass.getProperty());
        System.out.println(testClass.getTimeProperty());
    }
}
