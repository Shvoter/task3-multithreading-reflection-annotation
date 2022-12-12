package org.example;

import java.io.IOException;


public class App {
    public static void main( String[] args ) throws IOException, IllegalAccessException {
        String finesFolder = "fines";
        String statisticsOfFines = "fines\\statistics\\statistics.xml";
        int numberOfTreads = 4;
        //Check README.md to understand the time results of using different numbers of threads
        parallelStatistics.parallelParsingStatistics(finesFolder, statisticsOfFines, numberOfTreads);
    }
}
