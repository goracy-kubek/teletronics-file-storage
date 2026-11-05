# Teletronics File Storage

[![CI](https://github.com/goracy-kubek/teletronics-file-uploader/actions/workflows/ci.yml/badge.svg)](https://github.com/goracy-kubek/teletronics-file-uploader/actions/workflows/ci.yml)
![Java 25](https://img.shields.io/badge/Java-25-orange)
![Spring Boot 4.1](https://img.shields.io/badge/Spring%20Boot-4.1-brightgreen)
![MongoDB 7](https://img.shields.io/badge/MongoDB-7-green)

A RESTful API for storing and sharing files of any size, built with Spring Boot and MongoDB GridFS.

## Overview

STORAGE lets users upload files, label them with tags, make them public or private, and download via links.

The service has no user or session management. The caller identifies the user with the `X-User-Id` header and is
trusted to provide it.

## Features

- **Upload** a file with a filename, `PUBLIC`/`PRIVATE` visibility, up to 5 tags and an optional content type.
  Uploads are **streamed** to MongoDB GridFS, so the file is never held in memory or on local disk, whatever its size.
- **Duplicate prevention per user:** a file is rejected with `409` if the same user already has a file with the same
  filename **or** the same content (SHA-256). There is no global uniqueness across users.
- **Rename** a file.
- **List** the user's own files, or all public files. Both lists support:
  - filtering by tag (exact match, case-insensitive: `TAG` = `tAg`),
  - sorting by filename, upload date, tag, content type or size,
  - pagination.
- **Delete** files, only by their owner.
- **Download links:** every upload returns a unique, random, relative link (`/download/{token}`). Anyone who has the
  link can download the file, public or private.
- **Tags** are created on first use and normalized to lower case.

## Tech Stack

| Area | Choice |
|------|--------|
| Language / runtime | Java 25 |
| Framework | Spring Boot 4.1 (Web MVC, Data MongoDB, Validation, Actuator) |
| Storage | MongoDB 7: metadata in a collection, file content in GridFS |
| API contract | OpenAPI 3 ([`openapi.yaml`](storage-service/src/main/resources/static/openapi.yaml)); Java interfaces and models are generated from it at build time |
| Mapping | MapStruct |
| Content type detection | Apache Tika |
| API docs UI | springdoc-openapi |
| Build | Gradle (Kotlin DSL) |
| Tests | JUnit 5, Mockito, AssertJ, Testcontainers |
| Packaging | Docker (multi-stage build), Docker Compose |
| CI | GitHub Actions |

### How it works

- Commands (upload, rename, delete) and queries (download, list) are separate services:
  `FileCommandService` and `FileQueryService`.
- Data storage is separated behind contract interfaces, so MongoDB can be replaced by another storage without
  changing the services. File content and file metadata are stored separately:

  | Interface | Responsibility | MongoDB implementation |
  |-----------|----------------|------------------------|
  | `FileContentRepository` | File content: `store` (streamed), `download` (streamed), `delete` | `MongoFileContentRepository` (GridFS) |
  | `FileMetadataRepository` | Metadata changes: `reserve`, `complete`, `discard`, `deleteOwned`, `rename` | `MongoFileMetadataRepository` |
  | `FileQueryRepository` | Metadata reads: `findReadyByToken`, `findOwn`, `findPublic` (filter, sort, paging) | `MongoFileQueryRepository` |

  The interfaces are in [`repository/`](storage-service/src/main/java/com/teletronics/storage/repository) and the MongoDB
  implementations in `repository/mongo/`. Contract tests run against the interfaces, so another implementation can be
  checked with the same tests.
- An upload first **reserves** the filename as a `PENDING` record, streams the content to GridFS, and then marks the
  record `READY`. Unique indexes on `(owner, filename)`, `(owner, sha256)` and the download token make concurrent
  duplicate uploads fail with `409` instead of creating two files. An upload that is interrupted or fails discards
  its reservation, so the filename can be used again.
- Diagrams (PlantUML) are in [`diagrams/`](diagrams): use cases, containers, domain model, database and the upload activity.

## Prerequisites

| For | You need |
|-----|----------|
| Running the service | Docker with Docker Compose |
| Building and testing locally | JDK 25, and Docker (the tests start MongoDB with Testcontainers) |

## Installation

```bash
git clone git@github.com:goracy-kubek/teletronics-file-uploader.git
cd teletronics-file-uploader
```

Configuration for Docker Compose is in [`infrastructure/.env`](infrastructure/.env). Set at least the MongoDB credentials:

```dotenv
MONGO_ROOT_USERNAME=<username>
MONGO_ROOT_PASSWORD=<password>
```

Start MongoDB and the application:

```bash
cd infrastructure
docker compose up --build
```

The API is then available at `http://localhost:8080`. Check it with:

```bash
curl http://localhost:8080/actuator/health
```

Interactive API docs: <http://localhost:8080/swagger-ui.html>.

Stop everything with `docker compose down` (add `-v` to also delete the stored data).

### Container limits

The `app` container runs with a **1 GB memory limit** (`mem_limit: 1g`), a **read-only root filesystem**, and a
**64 MB** in-memory `/tmp`. Uploaded files are stored in MongoDB, not on the application's disk.

## Quick Start

All requests need the `X-User-Id` header. Examples use the user `alice`.

**Upload a file** (the body is the raw file content; metadata goes in the query string):

```bash
curl -X POST "http://localhost:8080/api/files?filename=report.pdf&visibility=PUBLIC&tags=work&tags=2026" \
  -H "X-User-Id: alice" \
  -H "Content-Type: application/octet-stream" \
  --data-binary @report.pdf
```

Response `201`:

```json
{
  "id": "<FILE_ID>",
  "filename": "report.pdf",
  "visibility": "PUBLIC",
  "tags": ["work", "2026"],
  "contentType": "application/pdf",
  "size": 1048576,
  "uploadDate": "2026-11-05T13:30:00Z",
  "downloadUrl": "/download/<TOKEN>"
}
```

Two values in the response are generated by the service for every upload. Copy them from **your** response and use them in the commands below:

| Placeholder | Where to find it | Used for |
|-------------|------------------|----------|
| `<FILE_ID>` | the `id` field | renaming and deleting the file |
| `<TOKEN>` | the end of the `downloadUrl` field, after `/download/` | downloading the file |

**Download by link** (no `X-User-Id` needed; the link is the access token):

```bash
curl -OJ "http://localhost:8080/download/<TOKEN>"
```

**List your files**, filtered by tag, sorted by size, 10 per page:

```bash
curl "http://localhost:8080/api/files?tag=work&sort=size,desc&page=0&size=10" -H "X-User-Id: alice"
```

**List all public files:**

```bash
curl "http://localhost:8080/api/files/public"
```

**Rename a file:**

```bash
curl -X PATCH "http://localhost:8080/api/files/<FILE_ID>" \
  -H "X-User-Id: alice" -H "Content-Type: application/json" \
  -d '{"filename": "report-final.pdf"}'
```

**Delete a file:**

```bash
curl -X DELETE "http://localhost:8080/api/files/<FILE_ID>" -H "X-User-Id: alice"
```

## API Reference

The full contract is in [`openapi.yaml`](storage-service/src/main/resources/static/openapi.yaml).
When the application is running, you can explore and try the API in **[Swagger UI](http://localhost:8080/swagger-ui.html)**
(`http://localhost:8080/swagger-ui.html`). The raw OpenAPI document is at `http://localhost:8080/v3/api-docs`.

| Method | Path | Description | Success |
|--------|------|-------------|---------|
| `POST` | `/api/files` | Upload a file | `201` |
| `GET` | `/api/files` | List the caller's files | `200` |
| `GET` | `/api/files/public` | List all public files | `200` |
| `PATCH` | `/api/files/{fileId}` | Change the filename | `200` |
| `DELETE` | `/api/files/{fileId}` | Delete an own file | `204` |
| `GET` | `/download/{token}` | Download by link | `200` |

### Upload parameters

| Parameter | In | Required | Description |
|-----------|----|----------|-------------|
| `X-User-Id` | header | yes | Id of the calling user |
| `filename` | query | yes | 1–255 characters |
| `visibility` | query | yes | `PUBLIC` or `PRIVATE` |
| `tags` | query | no | Up to 5, 1–50 characters each. Repeat the parameter: `tags=work&tags=2026` |
| `contentType` | query | no | MIME type. If omitted, it is detected from the content |

### List parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `tag` | none | Filter by one tag (exact, case-insensitive). An unknown tag gives an empty page |
| `sort` | `uploadDate,desc` | `field[,asc\|desc]`. Fields: `filename`, `uploadDate`, `tag`, `contentType`, `size` |
| `page` | `0` | Zero-based page number |
| `size` | `20` | Results per page, 1–100 |

### Errors

Errors use the RFC 7807 problem format (`application/problem+json`).

| Status | When |
|--------|------|
| `400` | Missing or invalid parameters, more than 5 tags, missing `X-User-Id`, interrupted upload |
| `404` | File not found or not owned by the caller; unknown download token |
| `409` | The user already has a file with the same filename or the same content |

## Configuration

| Variable | Used by | Default | Description |
|----------|---------|---------|-------------|
| `MONGODB_URI` | application | `mongodb://localhost:27017/storage` | MongoDB connection string. Compose sets it from the credentials below |
| `MONGO_ROOT_USERNAME` | Compose | required | MongoDB root user |
| `MONGO_ROOT_PASSWORD` | Compose | required | MongoDB root password |
| `MONGO_PORT` | Compose | `27017` | Host port for MongoDB |
| `APP_PORT` | Compose | `8080` | Host port for the API |

## Development

Run the tests (Docker must be running):

```bash
cd storage-service
./gradlew test
```

Run the application against a local MongoDB without Docker Compose:

```bash
cd storage-service
MONGODB_URI="mongodb://localhost:27017/storage" ./gradlew bootRun
```

### Test suite

| Kind | Examples |
|------|----------|
| Unit tests | services, controller, exception handler, utilities (Mockito) |
| Repository tests | the same contract tests run against MongoDB (Testcontainers) |
| Integration tests (`*IT`) | full HTTP flow against a random port with a real MongoDB |

The integration tests cover the main scenarios:

- parallel uploads with the same filename, and with the same content;
- upload of a file larger than 2 GiB, streamed and checked by SHA-256 on download;
- deleting and renaming another user's file;
- listing all public files;
- an upload cut off midway.

`LargeFileIT` is tagged `slow` and needs about 2 GB of free disk space for MongoDB. It takes under a minute on a fast machine.

### Continuous integration

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs on pushes to `main` and `dev` and on pull requests. It builds
and tests the project with Gradle, then builds the Docker image ([`infrastructure/Dockerfile`](infrastructure/Dockerfile)).
The image is not pushed anywhere.

## Contributing

1. Create a branch from `dev`.
2. Make your change, with tests.
3. Run `./gradlew build` in `storage-service`.
4. Open a pull request. CI must pass.

## License

No license has been chosen for this project yet.
