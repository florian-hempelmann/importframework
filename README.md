# Import Framework

My self-made Spring Boot based import framework migrated from a legacy Bloomreach Enterprise / JCR environment 
to a standalone application using SQLite. 

Originally, this project was a Maven submodule integrated into the Bloomreach lifecycle. Even in the original 
implementation, I focused on keeping the core import logic as independent as possible from the underlying platform. 
This made the import functionality easier to maintain and allowed feature changes to be implemented without tightly 
coupling the business logic to Bloomreach or JCR.

The project focuses on a configurable, backend-only import pipeline for Excel files.

## Tech Stack

* Java 21
* Spring Boot 3.5
* Maven
* SQLite
* Spring JDBC
* Apache POI
* YAML-based mapping configuration
* JUnit 5 / Spring MockMvc

## Architecture

The import pipeline is structured as:

```text
HTTP Multipart Upload
        ↓
ImportController
        ↓
ImportService
        ↓
Parser
        ↓
Mapping + Validation
        ↓
Enrichment
        ↓
UpdateStrategy
        ↓
ImportRepository
        ↓
SQLite
        ↓
ImportReport
```

The application currently supports a full replacement strategy:

```text
replaceFolder
    ↓
DELETE existing records
    ↓
INSERT imported records
```

This replaces the previous JCR/Bloomreach persistence layer while keeping the core parser, mapping, validation and enrichment concepts.

## Configuration

Import mappings are defined in YAML:

```text
src/main/resources/mappings/
├── wheretobuy-bloomreach.yaml (old config - not usable in this version)
└── wheretobuy-sqlite.yaml
```

The mapping defines:

* source column names
* target properties
* validators
* duplicate/first-occurrence flags
* target table
* import strategy
* (potentially enrichment)

Example:

```yaml
columns:
  - sourceName: shopname
    targetProperty: name
    validators:
      - required
```

## REST API

Import an Excel file with:

```text
POST /api/imports/{type}
```

Example:

```text
POST /api/imports/wheretobuy-sqlite
Content-Type: multipart/form-data

file = valid.xlsx
```

The response is an `ImportReport` containing:

* number of records read
* successful writes
* failed records
* skipped records
* validation/import errors
* execution metadata

## Database

The application uses SQLite.

Database:

```text
data/import-framework.db
```

Schema initialization is handled by:

```text
src/main/resources/schema.sql
```

The current target table is:

```text
wheretobuy
```

## Running the Application

Start the application with Maven:

```bash
mvn spring-boot:run
```

Run all tests:

```bash
mvn test
```

## Tests

The project contains tests for:

* import service behavior
* validation errors
* replace-folder imports
* SQLite persistence
* Spring application context
* REST multipart uploads
* invalid files
* unknown import types
* unsupported file extensions
* empty uploads

### You can test the import and receiving json report manually with:
```bash
curl -X POST   
-F "file=@src/main/resources/samples/wheretobuy.xlsx"   
http://localhost:8080/api/imports/wheretobuy-sqlite
```

## Project Structure

```text
src/main/java/org/mycompany
├── api/
 ├── ImportController.java          #after migration
 └── ImportExceptionHandler.java    #after migration
├── api-bloomreach-old/             #commented out classes regarding bloomreach
├── config/
├── enrichment/
├── mapping/
├── model/
├── parser/
├── persistence/
  └── SqliteImportRepository.java   #after migration
├── security-bloomreach-old/        #commented out classes regarding bloomreach
├── service/
├── strategy/
└── ImportFrameworkApplication.java #after migration
```

## Project Background

This project was originally developed by me during my vocational retraining in software development.

It started as an import framework for a Bloomreach Enterprise application and was delivered as a Maven submodule 
integrated into the Bloomreach lifecycle. From the beginning, I focused on keeping the core import logic as independent 
as possible from the CMS platform. This made the import functionality easier to maintain and allowed feature changes 
without tightly coupling the business logic to Bloomreach or JCR.

For this portfolio project, I migrated the framework into a standalone Spring Boot application and replaced the 
Bloomreach/JCR infrastructure with standard Spring Boot components and SQLite.

## AI-Assisted Development

AI tools were used as a development and learning aid during the migration.

I used AI to help understand unfamiliar Spring concepts, discuss architectural decisions, review code, identify 
potential issues and improve tests and documentation. The implementation, decisions and integration of the individual 
components remained under my responsibility.

This reflects how I currently use AI as a development tool: not as a replacement for understanding the code, but as 
an additional tool for learning, exploring alternatives and validating my approach.