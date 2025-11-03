import pytest
import sqlite3
import os
import pandas as pd
from pipeline import Pipeline, DB


@pytest.fixture
def test_db():
    """Fixture to create and cleanup test database"""
    db_file = 'test_db.sqlite'
    if os.path.exists(db_file):
        os.remove(db_file)
    yield db_file
    if os.path.exists(db_file):
        os.remove(db_file)


def test_pipeline_instantiation():
    """Test that Pipeline can be instantiated"""
    pipeline = Pipeline()
    assert pipeline is not None
    assert pipeline.population is None
    assert pipeline.unemployment is None


def test_extract():
    """Test extract method loads data into dataframes"""
    pipeline = Pipeline()
    pipeline.extract()
    
    assert pipeline.population is not None
    assert pipeline.unemployment is not None
    assert isinstance(pipeline.population, pd.DataFrame)
    assert isinstance(pipeline.unemployment, pd.DataFrame)
    assert len(pipeline.population) > 0
    assert len(pipeline.unemployment) > 0


def test_transform():
    """Test transform method properly transforms the data"""
    pipeline = Pipeline()
    pipeline.extract()
    pipeline.transform()
    
    expected_pop_cols = ['CBSA', 'MDIV', 'STCOU', 'NAME', 'LSAD', 'YEAR', 'POPULATION_EST']
    assert list(pipeline.population.columns) == expected_pop_cols
    
    expected_unemp_cols = ['FIPStxt', 'State', 'Area_name', 'Year', 'Unemployment_rate']
    assert list(pipeline.unemployment.columns) == expected_unemp_cols
    
    assert pipeline.population['YEAR'].dtype == object
    assert pipeline.unemployment['Year'].dtype == object


def test_load(test_db, monkeypatch):
    """Test load method inserts data into database"""
    pipeline = Pipeline()
    pipeline.extract()
    pipeline.transform()
    
    original_init = DB.__init__
    
    def mock_init(self, db_file=test_db):
        original_init(self, db_file)
    
    monkeypatch.setattr(DB, '__init__', mock_init)
    
    pipeline.load()
    
    conn = sqlite3.connect(test_db)
    cur = conn.cursor()
    
    pop_count = cur.execute("SELECT COUNT(*) FROM population").fetchone()[0]
    unemp_count = cur.execute("SELECT COUNT(*) FROM unemployment").fetchone()[0]
    
    assert pop_count > 0
    assert unemp_count > 0
    
    conn.close()


def test_pipeline_end_to_end(test_db, monkeypatch):
    """Test complete pipeline execution"""
    original_init = DB.__init__
    
    def mock_init(self, db_file=test_db):
        original_init(self, db_file)
    
    monkeypatch.setattr(DB, '__init__', mock_init)
    
    pipeline = Pipeline()
    pipeline.extract()
    pipeline.transform()
    pipeline.load()
    
    conn = sqlite3.connect(test_db)
    cur = conn.cursor()
    
    pop_count = cur.execute("SELECT COUNT(*) FROM population").fetchone()[0]
    unemp_count = cur.execute("SELECT COUNT(*) FROM unemployment").fetchone()[0]
    
    assert pop_count == 22312
    assert unemp_count == 65500
    
    pop_schema = cur.execute("PRAGMA table_info(population)").fetchall()
    unemp_schema = cur.execute("PRAGMA table_info(unemployment)").fetchall()
    
    assert len(pop_schema) == 7
    assert len(unemp_schema) == 5
    
    conn.close()
