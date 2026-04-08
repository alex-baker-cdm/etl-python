import sqlite3
import polars as pl


class Pipeline(object):
    def __init__(self):
        self.population = None
        self.unemployment = None

    def extract(self):
        """
        Data source (based on data from data.gov)
        description:
            table1: https://www2.census.gov/programs-surveys/popest/technical-documentation/file-layouts/2010-2017/cbsa-est2017-alldata.pdf
            table2: https://www.ers.usda.gov/data-products/county-level-data-sets/download-data
        """
        #url_popul_est = 'https://www2.census.gov/programs-surveys/popest/datasets/2010-2017/metro/totals/cbsa-est2017-alldata.csv'
        url_popul_est = 'data/cbsa-est2017-alldata.csv'
        #url_unemployment = 'https://www.ers.usda.gov/webdocs/DataFiles/48747/Unemployment.xls'
        url_unemployment = 'data/Unemployment.xls'

        self.population = pl.read_csv(url_popul_est, encoding='ISO-8859-1')
        self.unemployment = pl.read_excel(url_unemployment, read_options={"header_row": 7}, engine="calamine")

    def transform(self):
        # formatting Population dataset

        # keep the relevant columns only i.e. the columns that contain year-population-estimate and index names
        pop_idx = ['CBSA', 'MDIV', 'STCOU', 'NAME', 'LSAD']
        pop_cols = [c for c in self.population.columns if c.startswith('POPEST')]
        population = self.population.select(pop_idx + pop_cols)

        # melt, "unpivot" the yearly rate values (from wide format 'columns' to long format 'rows')
        self.population = population.unpivot(index=pop_idx,
                                             on=pop_cols,
                                             variable_name='YEAR',
                                             value_name='POPULATION_EST')

        # fix columns values
        self.population = self.population.with_columns(
            pl.col('YEAR').str.slice(-4).alias('YEAR')
        )  # e.g. POPESTIMATE2010 -> 2010

        # formatting Unemployment dataset

        # keep the relevant columns only i.e. unemployment-rate-year and names
        unemp_idx = ['FIPStxt', 'State', 'Area_name']
        unemp_cols = [c for c in self.unemployment.columns if c.startswith('Unemployment_rate')]
        unemployment = self.unemployment.select(unemp_idx + unemp_cols)

        # melt, "unpivot" the yearly rate values (from wide format 'columns' to long format 'rows')
        self.unemployment = unemployment.unpivot(index=unemp_idx,
                                                 on=unemp_cols,
                                                 variable_name='Year',
                                                 value_name='Unemployment_rate')

        # fix columns values
        self.unemployment = self.unemployment.with_columns(
            pl.col('Unemployment_rate').round(1)
        )  # set precision to .1
        self.unemployment = self.unemployment.with_columns(
            pl.col('Year').str.slice(-4).alias('Year')
        )  # remove prefix i.e. 'Unemployment_rate_XXXX'

    def load(self):
        db = DB()
        rows = self.population.rows()
        cols = self.population.columns
        placeholders = ', '.join(['?'] * len(cols))
        db.cur.executemany(
            f"INSERT INTO population ({', '.join(cols)}) VALUES ({placeholders})",
            rows
        )
        rows = self.unemployment.rows()
        cols = self.unemployment.columns
        placeholders = ', '.join(['?'] * len(cols))
        db.cur.executemany(
            f"INSERT INTO unemployment ({', '.join(cols)}) VALUES ({placeholders})",
            rows
        )
        db.conn.commit()


class DB(object):

    def __init__(self, db_file='db.sqlite'):
        self.conn = sqlite3.connect(db_file)
        self.cur = self.conn.cursor()
        self.__init_db()

    def __del__(self):
        self.conn.commit()
        self.conn.close()

    def __init_db(self):
        table1 = f"""CREATE TABLE IF NOT EXISTS population(
              CBSA INTEGER,
              MDIV REAL,
              STCOU INTEGER,
              NAME TEXT,
              LSAD TEXT,
              YEAR INTEGER,
              POPULATION_EST INTEGER
                );"""

        table2 = f"""CREATE TABLE IF NOT EXISTS unemployment(
            FIPStxt INTEGER,
            State TEXT,
            Area_name TEXT,
            Year INTEGER,
            unemployment_rate REAL
            );"""

        self.cur.execute(table1)
        self.cur.execute(table2)


if __name__ == '__main__':
    pipeline = Pipeline()
    print('Data Pipeline created')
    print('\t extracting data from source .... ')
    pipeline.extract()
    print('\t formatting and transforming data ... ')
    pipeline.transform()
    print('\t loading into database ... ')
    pipeline.load()

    print('\nDone. See: result in "db.sqlite"')
