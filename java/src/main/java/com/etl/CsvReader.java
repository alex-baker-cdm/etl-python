package com.etl;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import tech.tablesaw.api.Table;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class CsvReader {

    public static Table readPopulationData(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath);
             InputStreamReader isr = new InputStreamReader(fis, Charset.forName("ISO-8859-1"));
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(isr)) {

            List<String> headers = parser.getHeaderNames();
            List<List<String>> data = new ArrayList<>();
            
            for (CSVRecord record : parser) {
                List<String> row = new ArrayList<>();
                for (String header : headers) {
                    row.add(record.get(header));
                }
                data.add(row);
            }

            Table table = Table.create("population");
            
            for (int i = 0; i < headers.size(); i++) {
                String header = headers.get(i);
                List<String> columnData = new ArrayList<>();
                for (List<String> row : data) {
                    columnData.add(row.get(i));
                }
                
                if (isNumericColumn(columnData)) {
                    if (header.equals("MDIV")) {
                        DoubleColumn column = DoubleColumn.create(header);
                        for (String value : columnData) {
                            if (value == null || value.trim().isEmpty()) {
                                column.appendMissing();
                            } else {
                                try {
                                    column.append(Double.parseDouble(value));
                                } catch (NumberFormatException e) {
                                    column.appendMissing();
                                }
                            }
                        }
                        table.addColumns(column);
                    } else {
                        IntColumn column = IntColumn.create(header);
                        for (String value : columnData) {
                            if (value == null || value.trim().isEmpty()) {
                                column.appendMissing();
                            } else {
                                try {
                                    column.append(Integer.parseInt(value));
                                } catch (NumberFormatException e) {
                                    column.appendMissing();
                                }
                            }
                        }
                        table.addColumns(column);
                    }
                } else {
                    StringColumn column = StringColumn.create(header, columnData);
                    table.addColumns(column);
                }
            }
            
            return table;
            
        } catch (IOException e) {
            throw new RuntimeException("Error reading CSV file: " + filePath, e);
        }
    }

    private static boolean isNumericColumn(List<String> data) {
        int numericCount = 0;
        int totalCount = 0;
        
        for (String value : data) {
            if (value != null && !value.trim().isEmpty()) {
                totalCount++;
                try {
                    Double.parseDouble(value);
                    numericCount++;
                } catch (NumberFormatException e) {
                }
            }
        }
        
        return totalCount > 0 && (numericCount * 1.0 / totalCount) > 0.8;
    }
}
