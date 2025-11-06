package com.etl;

import tech.tablesaw.api.Table;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== ETL Pipeline - Java Implementation ===");
        System.out.println("Data Pipeline created");
        
        Pipeline pipeline = new Pipeline();
        
        System.out.println("\nExtracting data from source ...");
        pipeline.extract();
        
        System.out.println("\n=== Population Data Sample ===");
        Table population = pipeline.getPopulation();
        System.out.println("Columns: " + population.columnNames());
        System.out.println("Row count: " + population.rowCount());
        System.out.println("\nFirst 5 rows:");
        System.out.println(population.first(5));
        
        System.out.println("\n=== Unemployment Data Sample ===");
        Table unemployment = pipeline.getUnemployment();
        System.out.println("Columns: " + unemployment.columnNames());
        System.out.println("Row count: " + unemployment.rowCount());
        System.out.println("\nFirst 5 rows:");
        System.out.println(unemployment.first(5));
        
        System.out.println("\n=== Extraction Complete ===");
        System.out.println("Population data: ~" + population.rowCount() + " rows loaded");
        System.out.println("Unemployment data: ~" + unemployment.rowCount() + " rows loaded");
        System.out.println("\nNote: Transform and load operations are not implemented in this version.");
        System.out.println("This implementation focuses on data extraction only.");
    }
}
