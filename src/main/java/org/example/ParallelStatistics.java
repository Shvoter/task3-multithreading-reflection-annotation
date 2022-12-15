package org.example;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

/*
     Check README.md to understand the time results of using different numbers of threads
 */
public class ParallelStatistics {
     private static class Fines<K, V> extends LinkedHashMap<K, V> { }

     /**
      * This method parses a statistic by type of offenses by several Threads
      * @param inputFolderName - Path to a folder with json files with info about offenses.
      * @param outputXmlFileName - Path to an XML file to result of parsing.
      * @param numberOfTreads - number of threads for parsing
      */
     public static void parallelParsingStatistics(String inputFolderName, String outputXmlFileName, int numberOfTreads) throws IOException {
          List<CompletableFuture<Map<String, Double>>> completableFutures = new ArrayList<>();
          Map<String, Double> globalStatistics;
          ExecutorService executorService = Executors.newFixedThreadPool(numberOfTreads);

          File folder = new File(inputFolderName);
          File[] listOfFiles;

          if (!folder.isDirectory()) {
               throw new IllegalArgumentException("inputFolderName is not a name of folder");
          }

          listOfFiles = folder.listFiles();
          for (File file : listOfFiles) {
               if (file.isFile() && file.getPath().endsWith(".json")) {
                    for (int i = 0; i < 200; i++) {  //emulate big data
                         completableFutures.add(getCompletableStatistics(file, executorService));
                    }
               }
          }
          globalStatistics = getSortedMapByValue(getGlobalStatistics(completableFutures));

          mapStatisticToXml(globalStatistics, outputXmlFileName);
          executorService.shutdown();
     }

     private static Map<String, Double> getSortedMapByValue(Map<String, Double> map) {
          return map.entrySet().stream()
                  .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                  .collect(Collectors.toMap(
                          Map.Entry :: getKey, Map.Entry :: getValue, (oldValue, newValue) -> oldValue, Fines::new)
                  );
     }

     private static void mapStatisticToXml(Map<String, Double> statistics, String outputXmlFileName) throws IOException {
          XmlMapper xmlMapper = new XmlMapper();
          xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
          xmlMapper.configure( ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
          xmlMapper.writeValue(new File(outputXmlFileName), statistics);
     }

     private static Map<String, Double> getGlobalStatistics(List<CompletableFuture<Map<String, Double>>> completableFutures) {
               return completableFutures.stream()
                       .map(o -> {
                            try {
                                 return o.get();
                            } catch (InterruptedException | ExecutionException e) {
                                 throw new RuntimeException(e);
                            }
                       })
                       .flatMap(o-> o.entrySet().stream())
                       .collect(groupingBy(Map.Entry::getKey, summingDouble(Map.Entry::getValue)));
     }

     private static CompletableFuture<Map<String, Double>> getCompletableStatistics(File file, ExecutorService executorService) {
          return CompletableFuture.supplyAsync(() -> {
               Map<String, Double> statisticsFromFile = new Fines<>();
               String currenFineType = null;
               Double currentFineAmount = null;

               try(JsonParser jParser = new JsonFactory().createParser(file)) {
                    while (jParser.nextToken() != JsonToken.END_ARRAY) {
                         String fieldName = jParser.getCurrentName();

                         if ("type".equals(fieldName)) {
                              jParser.nextToken();
                              currenFineType = jParser.getText();
                         }
                         if ("fine_amount".equals(fieldName)) {
                              jParser.nextToken();
                              currentFineAmount = jParser.getDoubleValue();
                         }
                         if (currenFineType != null && currentFineAmount != null) {
                              fillStatistics(statisticsFromFile, currenFineType, currentFineAmount);
                              currenFineType = null;
                              currentFineAmount = null;
                         }
                    }
               } catch (IOException e) {
                    throw new RuntimeException(e);
               }
               return statisticsFromFile;
          }, executorService);
     }

     private static void fillStatistics(Map<String, Double> statistics, String key, Double value) {
          if (statistics.containsKey(key)) {
               statistics.replace(key, statistics.get(key) + value);
          } else {
               statistics.put(key, value);
          }
     }
}
