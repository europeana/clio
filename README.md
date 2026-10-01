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

# Reporting REST API

The `clio-reporting-rest` module exposes the reporting endpoints described below. Set `BASE_URL` to the deployed service URL; the examples use `http://localhost:8080`, Spring Boot's default local address. The service may be configured to use a different host, port, or context path.

The API accepts and returns JSON for metadata and dataset queries, and UTF-8 CSV for report downloads. Use `Accept: text/csv` when downloading a report. Interactive API documentation is available from the service root (`/`, which redirects to Swagger UI); the OpenAPI document is available at `/v3/api-docs`.

```sh
BASE_URL=http://localhost:8080
```

## Endpoints

| Method and path | Description | Success response |
| --- | --- | --- |
| `GET /available-reports` | Lists saved reports, including report and batch IDs, creation time, and a URL to fetch each report by batch ID. | `200` JSON array of report details |
| `GET /report-by-batch-id?batchId={id}` | Downloads the saved report associated with the specified batch ID. | `200` CSV |
| `GET /reports?reportId={id}` | Downloads a saved report by its report ID. | `200` CSV |
| `GET /latest-report` | Downloads the latest saved report. A report may include links from multiple batches. | `200` CSV |
| `GET /batches?maxResults={count}` | Lists the most recent batches, newest first. `maxResults` is optional and defaults to `5`; it must be positive. | `200` JSON array |
| `POST /datasets` | Returns a filtered, paginated dataset summary and the available filter options. | `200` JSON object |
| `GET /runs?datasetId={id}` | Lists run summaries for one dataset, newest first. | `200` JSON array |
| `POST /runs/links/export` | Streams a CSV of links matching the supplied filters. | `200` CSV download |

### Report metadata and downloads

`GET /available-reports` returns entries with this shape:

```json
[
  {
    "reportId": 17,
    "batchId": 42,
    "creationTime": "2026-09-30T08:15:30Z",
    "url": "http://localhost:8080/report-by-batch-id?batchId=42"
  }
]
```

The `url` is generated from the current request's host/context and can be used directly to download that batch's report. Saved report endpoints respond with `Content-Type: text/csv`, a content-disposition filename, and UTF-8 CSV content.

```sh
# List stored report details
curl -H 'Accept: application/json' "$BASE_URL/available-reports"

# Download a report using its batch ID or report ID
curl -fL -H 'Accept: text/csv' \
  "$BASE_URL/report-by-batch-id?batchId=42" -o batch-42.csv
curl -fL -H 'Accept: text/csv' \
  "$BASE_URL/reports?reportId=17" -o report-17.csv

# Download the most recent report
curl -fL -H 'Accept: text/csv' \
  "$BASE_URL/latest-report" -o latest-report.csv
```

### Batch history

`GET /batches` returns a JSON array. Each entry contains:

| Field | Meaning |
| --- | --- |
| `creationTime` | Batch creation timestamp |
| `lastUpdateTimeInSolr` | Solr update timestamp recorded for the batch |
| `lastUpdateTimeInMetisCore` | Most recent Metis history indexing-task timestamp recorded for the batch |
| `datasetsExcludedAlreadyRunning` | Datasets omitted because they were still pending from an earlier batch; may be `null` if unknown |
| `datasetsExcludedNotIndexed` | Datasets omitted because their data was not indexed; may be `null` if unknown |
| `datasetsExcludedWithoutLinks` | Datasets omitted because they had no links to check; may be `null` if unknown |
| `datasetsProcessed` | Number of datasets/runs that finished processing |
| `datasetsPending` | Number of datasets/runs that have not finished processing |

Timestamps are ISO-8601 offset date-time strings. If `maxResults` is omitted, the endpoint returns up to five batches.

```sh
curl -H 'Accept: application/json' "$BASE_URL/batches"
curl -H 'Accept: application/json' "$BASE_URL/batches?maxResults=10"
```

A non-positive `maxResults` returns `400 Bad Request`.

### Dataset summaries and filtering

`POST /datasets` requires a JSON object with both `filters` and `pagination`. Filter properties may be omitted or set to `null` to leave that criterion unrestricted:

| Filter | Type | Meaning |
| --- | --- | --- |
| `provider` | array of strings | Include datasets matching any provider in the set |
| `dataProvider` | array of strings | Include datasets matching any data provider in the set |
| `datasetId` | array of strings | Include datasets matching any dataset ID in the set |
| `datasetName` | array of strings | Include datasets matching any dataset name in the set |
| `excludedDatasetId` | array of strings | Exclude the listed dataset IDs |
| `dateFrom`, `dateTo` | `yyyy-MM-dd` | Restrict by last-index date range; both endpoints are inclusive |
| `percentLinksInOperationFrom`, `percentLinksInOperationTo` | integer | Restrict by inclusive percentage range (0–100) |

For set-valued filters, a dataset may match any value in an individual set; separate filter criteria are combined. Pagination takes `offset` and `limit`. Negative or missing offsets are normalized to `0`; limits are normalized to the supported range `5`–`100` (missing or smaller than `5` becomes `5`, and values above `100` become `100`). The response pagination includes `hasMoreAvailable`, which indicates whether another page is available.

Example request:

```sh
curl -X POST "$BASE_URL/datasets" \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json' \
  -d '{
    "filters": {
      "provider": ["Example Provider"],
      "datasetId": ["http://data.example.org/dataset/1"],
      "dateFrom": "2026-01-01",
      "dateTo": "2026-09-30",
      "percentLinksInOperationFrom": 80,
      "percentLinksInOperationTo": 100
    },
    "pagination": {
      "offset": 0,
      "limit": 10
    }
  }'
```

The response has `results`, `filterOptions`, and `pagination`. Each result contains `datasetId`, `datasetName`, `datasetSize`, `datasetLastIndex` (`yyyy-MM-dd` or `null`), `provider`, `dataProvider`, and `percentLinksInOperation`. `filterOptions` gives the available `provider`, `dataProvider`, `datasetId`, and `datasetName` values for the submitted criteria; it also echoes the other filter criteria.

```json
{
  "results": [
    {
      "datasetId": "http://data.example.org/dataset/1",
      "datasetName": "Example collection",
      "datasetSize": 12000,
      "datasetLastIndex": "2026-08-15",
      "provider": "Example Provider",
      "dataProvider": "Example Data Provider",
      "percentLinksInOperation": 92
    }
  ],
  "filterOptions": {
    "provider": ["Example Provider"],
    "dataProvider": ["Example Data Provider"],
    "datasetId": ["http://data.example.org/dataset/1"],
    "datasetName": ["Example collection"]
  },
  "pagination": {
    "offset": 0,
    "limit": 10,
    "hasMoreAvailable": false
  }
}
```

The endpoint returns `400 Bad Request` when `filters` or `pagination` is missing.

### Dataset runs

`GET /runs` requires the `datasetId` query parameter. It returns a JSON array of that dataset's runs, in reverse chronological order. Each item has `runId`, `startingTime` (`yyyy-MM-dd`), and `percentLinksInOperation`.

```sh
curl -G -H 'Accept: application/json' \
  --data-urlencode 'datasetId=http://data.example.org/dataset/1' \
  "$BASE_URL/runs"
```

An empty or whitespace-only dataset ID returns `400 Bad Request`.

### Export filtered run links

`POST /runs/links/export` accepts a `FieldFilters` JSON object directly (unlike `POST /datasets`, there is no `filters` wrapper or pagination). It streams matching link records from the most recent 90-day checking window as a CSV attachment. The CSV columns are `Dataset ID`, `Dataset's Metis page`, `Dataset size`, `Provider`, `Data provider`, `Record ID`, `Last record index`, `Record edm:type`, `Record content tier`, `Record metadata tier`, `Link type`, `Link`, `Link server`, `Time of checking`, and `Error`.

Use the same filter names and types as the dataset-summary endpoint. For example:

```sh
curl -fL -X POST "$BASE_URL/runs/links/export" \
  -H 'Content-Type: application/json' \
  -H 'Accept: text/csv' \
  -d '{
    "provider": ["Example Provider"],
    "datasetId": ["http://data.example.org/dataset/1"],
    "dateFrom": "2026-01-01",
    "dateTo": "2026-09-30",
    "percentLinksInOperationFrom": 80
  }' \
  -o filtered-links.csv
```

## Errors

An error response with a body uses JSON shaped as `{"message":"..."}`. A missing saved report returns `404 Not Found`; persistence or other unhandled failures return `500 Internal Server Error`. Invalid input such as a missing required query parameter or non-positive `maxResults` returns `400 Bad Request`; some such validation responses may have an empty body.