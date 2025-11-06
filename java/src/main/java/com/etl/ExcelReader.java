package com.etl;

import org.apache.poi.ss.usermodel.*;
import tech.tablesaw.api.Table;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExcelReader {

    public static Table readUnemploymentData(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            
            Row headerRow = sheet.getRow(7);
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellValueAsString(cell));
            }

            List<List<String>> data = new ArrayList<>();
            for (int i = 8; i < sheet.getPhysicalNumberOfRows(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    List<String> rowData = new ArrayList<>();
                    for (int j = 0; j < headers.size(); j++) {
                        Cell cell = row.getCell(j);
                        rowData.add(getCellValueAsString(cell));
                    }
                    data.add(rowData);
                }
            }

            Table table = Table.create("unemployment");
            
            for (int i = 0; i < headers.size(); i++) {
                String header = headers.get(i);
                List<String> columnData = new ArrayList<>();
                for (List<String> row : data) {
                    columnData.add(row.get(i));
                }
                
                if (isNumericColumn(columnData)) {
                    if (header.equals("FIPStxt")) {
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
                    } else {
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
                    }
                } else {
                    StringColumn column = StringColumn.create(header, columnData);
                    table.addColumns(column);
                }
            }
            
            return table;
            
        } catch (IOException e) {
            throw new RuntimeException("Error reading Excel file: " + filePath, e);
        }
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return "";
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
