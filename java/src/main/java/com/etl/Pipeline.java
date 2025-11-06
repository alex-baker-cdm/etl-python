package com.etl;

import tech.tablesaw.api.Table;

public class Pipeline {
    private Table population;
    private Table unemployment;

    public Pipeline() {
        this.population = null;
        this.unemployment = null;
    }

    public void extract() {
        String populationPath = "../data/cbsa-est2017-alldata.csv";
        String unemploymentPath = "../data/Unemployment.xls";

        System.out.println("Extracting data from sources...");
        
        this.population = CsvReader.readPopulationData(populationPath);
        System.out.println("Population data loaded: " + population.rowCount() + " rows, " + population.columnCount() + " columns");
        
        this.unemployment = ExcelReader.readUnemploymentData(unemploymentPath);
        System.out.println("Unemployment data loaded: " + unemployment.rowCount() + " rows, " + unemployment.columnCount() + " columns");
    }

    public Table getPopulation() {
        return population;
    }

    public Table getUnemployment() {
        return unemployment;
    }
}
