package uj.wmii.pwj.w7.insurance;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

public class FloridaInsurance {

    private static List<InsuranceEntry> getInsuranceEntries(String pathName, String fileName) throws IOException {
        try (ZipFile zip = new ZipFile(new File(pathName), ZipFile.OPEN_READ);
             BufferedReader reader = new BufferedReader(new InputStreamReader(
                     zip.getInputStream(zip.getEntry(fileName)), StandardCharsets.UTF_8
             ))) {
            reader.readLine();
            List<InsuranceEntry> entries = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                entries.add(new InsuranceEntry(line));
            }
            return entries;
        }
    }


    public static void main(String[] args) {
        String zipName = "FL_insurance.csv.zip";
        String fileName = "FL_insurance.csv";
        List<InsuranceEntry> insuranceEntryList = null;
        try {
            insuranceEntryList = getInsuranceEntries(zipName, fileName);
        } catch (IOException e) {
            System.err.println("Can't read " + zipName + ": " + e.getMessage());
        }
        Objects.requireNonNull(insuranceEntryList);

        int count = insuranceEntryList.stream().map((entry) -> entry.county).distinct().toArray().length;
        Path countFile = Paths.get("count.txt");
        try {
            Files.writeString(countFile, Integer.toString(count));
        } catch (IOException e) {
            System.err.println("Can't write to " + countFile.getFileName());
        }

        double sum = insuranceEntryList.stream().mapToDouble((entry) -> entry.tiv2012).sum();
        Path sumFile = Paths.get("tiv2012.txt");
        try {
            Files.writeString(sumFile, String.format(Locale.ROOT,"%.2f", sum));
        } catch (IOException e) {
            System.err.println("Can't write to " + sumFile.getFileName());
        }

        List<Map.Entry<String, Double>> list =
                insuranceEntryList.stream()
                        .collect(Collectors.groupingBy(
                            InsuranceEntry::getCounty,
                            Collectors.summingDouble(InsuranceEntry::getDifference)
                        ))
                        .entrySet().stream()
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                        .limit(10)
                        .toList();
        StringBuilder mostValuable = new StringBuilder();
        mostValuable.append("country,value");
        for (Map.Entry<String, Double> e : list) {
            mostValuable.append(String.format(Locale.ROOT, "%n%s,%.2f", e.getKey(), e.getValue()));
        }
        Path mostValuableFile = Paths.get("most_valuable.txt");
        try {
            Files.writeString(mostValuableFile, mostValuable.toString());
        } catch (IOException e) {
            System.err.println("Can't write to " + mostValuableFile.getFileName());
        }
    }
}


class InsuranceEntry {
    String county;
    double tiv2011;
    double tiv2012;

    InsuranceEntry(String line) {
        String[] values = line.split(",");
        county = values[2];
        tiv2011 = Double.parseDouble(values[7]);
        tiv2012 = Double.parseDouble(values[8]);
        assert values.length == 18;
    }

    String getCounty() {
        return county;
    }

    Double getDifference() {
        return tiv2012 - tiv2011;
    }


}
