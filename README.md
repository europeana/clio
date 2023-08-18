[![CI](https://github.com/europeana/clio/actions/workflows/ci.yml/badge.svg)](https://github.com/europeana/clio/actions/workflows/ci.yml) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=europeana_clio&metric=coverage)](https://sonarcloud.io/summary/new_code?id=europeana_clio)

[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=europeana_clio&metric=bugs)](https://sonarcloud.io/summary/new_code?id=europeana_clio) [![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=europeana_clio&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=europeana_clio)  
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=europeana_clio&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=europeana_clio) [![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=europeana_clio&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=europeana_clio)  
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=europeana_clio&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=europeana_clio) [![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=europeana_clio&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=europeana_clio)  
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=europeana_clio&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=europeana_clio)

# Clio

Clio, named after the Muse of History, is the automatic link checking tool (Checking Links In 
Operation) that runs regularly to signal whether link rot has set in.

# Creating the database tables
If a database does not exist, it should be manually created or the parameter `createDatabaseIfNotExist=true` on the connection url should be set.
Creating the tables should be performed through using the hibernate property `hibernate.hbm2ddl.auto=create-only`.