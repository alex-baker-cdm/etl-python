# API Documentation

## Overview

This document describes the API surface of the ETL Python project. This project is a **batch ETL (Extract, Transform, Load) data pipeline** for processing US demographic and economic statistics.

## HTTP/REST APIs

**Status: None**

This repository does not contain any HTTP/REST APIs. The application is a batch processing pipeline executed via command line, not a web service. Therefore, no OpenAPI/Swagger specification is applicable.

The codebase was reviewed and confirmed to contain no web framework dependencies (Flask, FastAPI, Django, etc.) and no HTTP endpoints.

## Programmatic API

The ETL pipeline exposes the following Python classes and methods as its programmatic interface:

### Pipeline Class

Located in `pipeline.py`, the `Pipeline` class orchestrates the ETL process.

#### Constructor

```python
Pipeline()
```

Creates a new Pipeline instance with `population` and `unemployment` attributes initialized to `None`.

#### Methods

| Method | Description | Parameters | Returns |
|--------|-------------|------------|---------|
| `extract()` | Reads raw data from CSV and Excel source files into pandas DataFrames. Populates `self.population` from `data/cbsa-est2017-alldata.csv` and `self.unemployment` from `data/Unemployment.xls`. | None | None |
| `transform()` | Transforms data from wide format (yearly columns) to long format (year as rows). Applies data cleaning including year extraction and rounding. | None | None |
| `load()` | Persists transformed DataFrames to SQLite database (`db.sqlite`) using the `DB` class. | None | None |

#### Usage Example

```python
from pipeline import Pipeline

pipeline = Pipeline()
pipeline.extract()    # Load data from source files
pipeline.transform()  # Transform to analysis-ready format
pipeline.load()       # Save to SQLite database
```

### DB Class

Located in `pipeline.py`, the `DB` class manages SQLite database connections and schema.

#### Constructor

```python
DB(db_file='db.sqlite')
```

Creates a database connection and initializes the schema with `population` and `unemployment` tables.

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `db_file` | str | `'db.sqlite'` | Path to the SQLite database file |

#### Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| `conn` | sqlite3.Connection | Database connection object |
| `cur` | sqlite3.Cursor | Database cursor for executing queries |

## Data Models

The ETL pipeline produces two database tables:

### Population Table

Stores US population estimates by metropolitan/micropolitan statistical area.

| Column | Type | Description |
|--------|------|-------------|
| CBSA | INTEGER | Core Based Statistical Area identifier |
| MDIV | REAL | Metropolitan Division identifier (nullable) |
| STCOU | INTEGER | State-County code (NULL for metro areas, value for counties) |
| NAME | TEXT | Area name |
| LSAD | TEXT | Legal/Statistical Area Description |
| YEAR | INTEGER | Year of estimate (2010-2017) |
| POPULATION_EST | INTEGER | Population estimate value |

### Unemployment Table

Stores county-level unemployment rates.

| Column | Type | Description |
|--------|------|-------------|
| FIPStxt | INTEGER | Federal Information Processing Standard county code |
| State | TEXT | State abbreviation |
| Area_name | TEXT | County/area name |
| Year | INTEGER | Year of measurement (2010-2017) |
| unemployment_rate | REAL | Unemployment rate percentage |

## Command Line Interface

The pipeline is executed via command line:

```bash
python pipeline.py
```

**Output:**
```
Data Pipeline created
	 extracting data from source ....
	 formatting and transforming data ...
	 loading into database ...

Done. See: result in "db.sqlite"
```

## Data Sources

- **Population Data**: US Census Bureau population estimates (`data/cbsa-est2017-alldata.csv`)
- **Unemployment Data**: USDA Economic Research Service (`data/Unemployment.xls`)

## Future Considerations

If HTTP/REST API functionality is needed in the future, consider:

1. Adding FastAPI or Flask to expose the ETL operations as endpoints
2. Creating endpoints such as:
   - `POST /pipeline/run` - Execute the full ETL pipeline
   - `GET /population` - Query population data
   - `GET /unemployment` - Query unemployment data
3. Generating OpenAPI/Swagger specification from the web framework

At that point, this directory would contain the appropriate `openapi.yaml` specification file.
