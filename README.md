# Vert.x REST API (Kotlin + JDK 21 + Gradle + jOOQ ORM + PostgreSQL)

Starter project REST API modern, reaktif, dan performan tinggi menggunakan **Kotlin 2.2+**, **Eclipse Vert.x 5**, **JDK 21**, **Gradle 9** (Kotlin DSL), **jOOQ ORM**, dan **PostgreSQL**.

---

## 📋 Daftar Isi
- [Tech Stack & Prasyarat](#-tech-stack--prasyarat)
- [Konfigurasi Profil & Database](#-konfigurasi-profil--database)
- [Database Migration (Liquibase)](#-database-migration-liquibase)
- [Struktur Direktori & Arsitektur](#-struktur-direktori--pola-arsitektur-berlapis)
- [Cara Menjalankan Aplikasi](#-cara-menjalankan-aplikasi)
  - [1. Mode Development](#1-menjalankan-di-mode-development)
  - [2. Menjalankan Unit Test](#2-menjalankan-unit-test)
  - [3. Build dan Menjalankan Fat JAR](#3-membangun-dan-menjalankan-fat-jar-produksi)
  - [4. Menjalankan Migrasi Database Mandiri](#4-menjalankan-migrasi-database-mandiri-cli)
- [Daftar Endpoint REST API](#-daftar-endpoint-rest-api)
- [Panduan Lengkap cURL & Contoh Request/Response](#-panduan-lengkap-curl--contoh-requestresponse)
  - [1. Health Check Service](#1-health-check-service)
  - [2. Mengambil Semua Items](#2-mengambil-semua-items)
  - [3. Menambahkan Item Baru](#3-menambahkan-item-baru)
  - [4. Mengambil Item Berdasarkan ID](#4-mengambil-item-berdasarkan-id)
  - [5. Menghapus Item](#5-menghapus-item)
  - [6. Pengujian Validasi Error 400 Bad Request](#6-pengujian-validasi-error-400-bad-request)
  - [7. Pengujian Endpoint Tidak Ada 404 Not Found](#7-pengujian-endpoint-tidak-ada-404-not-found)
- [Verifikasi Langsung ke Database PostgreSQL](#-verifikasi-langsung-ke-database-postgresql)

---

## 🛠️ Tech Stack & Prasyarat

- **Bahasa**: **Kotlin 2.2.20** (JVM Target 21)
- **Runtime**: OpenJDK 21 LTS
- **Build Tool**: Gradle 9.4+ (dilengkapi Gradle Wrapper `gradlew` / `gradlew.bat`)
- **Framework**: Eclipse Vert.x 5 (`vertx-core`, `vertx-web`, `vertx-lang-kotlin`, `vertx-launcher-application`)
- **Database**: PostgreSQL 16+ (Database default: `belajar-vertx-db`)
- **Database Migration**: **Liquibase 4.31.0** (YAML changelog & Gradle CLI task)
- **ORM / Query Builder**: **jOOQ 3.19.18** (Dialek PostgreSQL)
- **Connection Pool**: HikariCP 6.2.0
- **Configuration**: YAML (`application.yml`) & Environment (`.env` via `dotenv-java`)
- **Logging**: SLF4J + Logback (`logback-classic`)
- **Testing**: JUnit 5 + Vert.x JUnit 5 (`vertx-junit5`, `vertx-web-client`)
- **Packaging**: Gradle Shadow Plugin (`com.gradleup.shadow`) untuk menghasilkan Fat JAR

---

## ⚙️ Konfigurasi Profil & Database

Aplikasi mendukung konfigurasi ganda melalui **`src/main/resources/application.yml`** dan file **`.env`** di root direktori project. Nilai dari `.env` atau environment variables sistem secara otomatis akan meng-override nilai di `application.yml`.

### 1. `src/main/resources/application.yml` (Default Profile)
```yaml
server:
  port: 8080

database:
  host: localhost
  port: 5432
  user: root
  password: ""
  dbname: belajar-vertx-db
  pool:
    maxSize: 10
    minIdle: 2
    connectionTimeoutMs: 30000
    idleTimeoutMs: 600000
    maxLifetimeMs: 1800000
    keepaliveTimeMs: 300000
    leakDetectionThresholdMs: 0
```

### 2. File `.env` (Local Environment Overrides)
```env
# Server Config
SERVER_PORT=8080

# PostgreSQL Database Config
DB_HOST=localhost
DB_PORT=5432
DB_USER=root
DB_PASSWORD=
DB_NAME=belajar-vertx-db

# HikariCP Connection Pooling Config
DB_POOL_MAX_SIZE=10
DB_POOL_MIN_IDLE=2
DB_POOL_CONNECTION_TIMEOUT_MS=30000
DB_POOL_IDLE_TIMEOUT_MS=600000
DB_POOL_MAX_LIFETIME_MS=1800000
DB_POOL_KEEPALIVE_TIME_MS=300000
DB_POOL_LEAK_DETECTION_THRESHOLD_MS=0
```

| Parameter Environment | Default | Keterangan |
| :--- | :--- | :--- |
| `DB_POOL_MAX_SIZE` | `10` | Jumlah maksimal koneksi aktif & idle di dalam pool |
| `DB_POOL_MIN_IDLE` | `2` | Jumlah minimal koneksi idle yang selalu standby |
| `DB_POOL_CONNECTION_TIMEOUT_MS` | `30000` (30s) | Waktu tunggu maksimal untuk memperoleh koneksi sebelum timeout |
| `DB_POOL_IDLE_TIMEOUT_MS` | `600000` (10m) | Waktu maksimal sebuah koneksi boleh idle sebelum dipensiunkan |
| `DB_POOL_MAX_LIFETIME_MS` | `1800000` (30m) | Masa hidup total koneksi sebelum digantikan koneksi baru |
| `DB_POOL_KEEPALIVE_TIME_MS` | `300000` (5m) | Interval ping periodik untuk menjaga koneksi tetap hidup |
| `DB_POOL_LEAK_DETECTION_THRESHOLD_MS` | `0` (Off) | Ambang batas deteksi kebocoran koneksi (`0` = nonaktif) |

---

## 🗄️ Database Migration (Liquibase)

Aplikasi ini menggunakan **Liquibase** dengan format **Formatted SQL (`.sql`)** untuk mengelola versi dan migrasi skema database PostgreSQL secara aman, mudah dibaca, dan idempotent.

### Struktur Changelog
```text
src/main/resources/db/changelog/
├── db.changelog-master.yaml        # Master changelog (meng-include changeset SQL)
└── changes/
    └── 001-create-items-table.sql  # Changeset SQL murni tabel items
```

### Format File Migrasi (`.sql`)
File migrasi ditulis langsung dalam sintaks SQL standar PostgreSQL dengan komentar khusus Liquibase:
```sql
--liquibase formatted sql

--changeset teten:001-create-items-table
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'items';
CREATE TABLE IF NOT EXISTS items (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
--rollback DROP TABLE IF EXISTS items;
```

### Mekanisme Migrasi
1. **Otomatis saat Aplikasi Startup**:
   Ketika aplikasi dijalankan (`gradle run` atau via Fat JAR), `DatabaseManager` akan memanggil `MigrationManager.migrate(dataSource)` untuk mengeksekusi changelog yang belum dijalankan secara otomatis sebelum Verticle menerima request HTTP.
2. **Mandiri via Gradle CLI**:
   Dapat dijalankan kapan saja secara independen tanpa menyalakan HTTP server:
   ```powershell
   gradle liquibaseUpdate
   # atau menggunakan wrapper
   ./gradlew liquibaseUpdate
   ```

### Tabel Riwayat Liquibase
Liquibase secara otomatis membuat dan mengelola 2 tabel audit internal di PostgreSQL:
- **`databasechangelog`**: Menyimpan riwayat setiap changeset yang telah dieksekusi (id, author, timestamp, hash, status).
- **`databasechangeloglock`**: Mencegah race condition antar-instance aplikasi saat migrasi berjalan serentak.

---

## 📁 Struktur Direktori & Pola Arsitektur Berlapis

Project ini menerapkan pola arsitektur berlapis (**Controller - Service - Repository**):

```text
VertXRestApi/
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── com/example/restapi/
│   │   │       ├── MainVerticle.kt               # Composition Root & HTTP Server Lifecycle
│   │   │       ├── config/
│   │   │       │   └── AppConfig.kt              # Loader profil application.yml & .env
│   │   │       ├── controller/
│   │   │       │   ├── HealthController.kt       # HTTP Handler untuk /health & DB probe
│   │   │       │   └── ItemController.kt         # HTTP Handler untuk routing /api/v1/items
│   │   │       ├── db/
│   │   │       │   ├── DatabaseManager.kt        # HikariCP pool, auto-migrate, & jOOQ DSLContext
│   │   │       │   ├── MigrationManager.kt       # Runner migrasi Liquibase
│   │   │       │   └── MigrationCli.kt           # Entrypoint CLI untuk task gradle liquibaseUpdate
│   │   │       ├── dto/
│   │   │       │   └── CreateItemRequest.kt      # DTO Payload request dengan validasi
│   │   │       ├── exception/
│   │   │       │   └── AppException.kt           # NotFoundException (404) & BadRequestException (400)
│   │   │       ├── model/
│   │   │       │   └── Item.kt                   # Domain Entity Model (Data Class)
│   │   │       ├── repository/
│   │   │       │   ├── ItemRepository.kt         # Interface kontrak data access
│   │   │       │   └── ItemRepositoryImpl.kt     # Implementasi query database via jOOQ ORM
│   │   │       └── service/
│   │   │           ├── ItemService.kt            # Interface kontrak logika bisnis
│   │   │           └── ItemServiceImpl.kt        # Implementasi logika bisnis & aturan domain
│   │   └── resources/
│   │       ├── application.yml                   # Default profile configuration
│   │       ├── logback.xml                       # Konfigurasi logging console
│   │       └── db/
│   │           └── changelog/
│   │               ├── db.changelog-master.yaml  # Master changelog Liquibase
│   │               └── changes/
│   │                   └── 001-create-items-table.sql
│   └── test/
│       └── kotlin/
│           └── com/example/restapi/
│               └── MainVerticleTest.kt           # Automated tests dengan VertxExtension
├── .env                                          # Local environment overrides
├── .gitignore
├── build.gradle.kts                              # Dependencies & build tasks (incl. liquibaseUpdate)
├── gradlew
├── gradlew.bat
├── README.md
└── settings.gradle.kts
```

---

## 🚀 Cara Menjalankan Aplikasi

Pastikan service **PostgreSQL** sudah berjalan di `localhost:5432` dengan database `belajar-vertx-db`.

### 1. Menjalankan di Mode Development

Gunakan Gradle Wrapper yang sudah disertakan:

**Windows (PowerShell):**
```powershell
.\gradlew.bat run
# atau
./gradlew run
```

**Linux / macOS:**
```bash
./gradlew run
```

Server default akan aktif di port `8080` (`http://localhost:8080`).

---

### 2. Menjalankan Unit Test

Jalankan pengujian otomatis (automated unit tests):

```powershell
./gradlew test
```

Laporan HTML hasil test lengkap dapat dilihat di:
`build/reports/tests/test/index.html`

---

### 3. Membangun dan Menjalankan Fat JAR (Produksi)

Untuk mem-package seluruh aplikasi dan dependensinya menjadi satu file JAR executable:

```powershell
./gradlew shadowJar
```

File Fat JAR akan dihasilkan di:
`build/libs/vertx-rest-api-1.0.0-SNAPSHOT-fat.jar`

Jalankan file Fat JAR tersebut dengan Java 21:
```powershell
java -jar build/libs/vertx-rest-api-1.0.0-SNAPSHOT-fat.jar
```

---

### 4. Menjalankan Migrasi Database Mandiri (CLI)

Jika ingin menjalankan migrasi Liquibase ke database secara terpisah (misalnya pada pipeline CI/CD sebelum deployment aplikasi):

```powershell
./gradlew liquibaseUpdate
# atau
gradle liquibaseUpdate
```

Perintah ini akan membaca koneksi dari `.env` / `application.yml`, menjalankan changeset yang tertunda, dan mencatat riwayat ke tabel `databasechangelog`.

---

## 📡 Daftar Endpoint REST API

| Method | Endpoint | Deskripsi | Status Code |
| :--- | :--- | :--- | :--- |
| `GET` | `/health` | Health check server & koneksi PostgreSQL | `200 OK` / `503 Service Unavailable` |
| `GET` | `/api/v1/items` | Mengambil semua list item dari database | `200 OK` |
| `GET` | `/api/v1/items/:id` | Mengambil detail item berdasarkan ID | `200 OK` / `404 Not Found` |
| `POST` | `/api/v1/items` | Menambahkan item baru ke database | `201 Created` / `400 Bad Request` |
| `DELETE` | `/api/v1/items/:id` | Menghapus item dari database | `200 OK` / `404 Not Found` |

---

## 🧪 Panduan Lengkap cURL & Contoh Request/Response

### 1. Health Check Service
Memeriksa apakah server Vert.x berjalan dan koneksi ke database PostgreSQL aktif.

**Perintah cURL (PowerShell / Bash):**
```powershell
curl.exe -s http://localhost:8080/health
```

**Contoh Response (`200 OK`):**
```json
{
  "status" : "UP",
  "service" : "vertx-rest-api",
  "database" : {
    "status" : "UP",
    "type" : "PostgreSQL (jOOQ)",
    "host" : "localhost",
    "port" : 5432,
    "database" : "belajar-vertx-db"
  },
  "timestamp" : "2026-09-09T04:30:11.110250700Z"
}
```

---

### 2. Mengambil Semua Items
Mengambil seluruh data items yang ada di database PostgreSQL.

**Perintah cURL:**
```powershell
curl.exe -s http://localhost:8080/api/v1/items
```

**Contoh Response (`200 OK`):**
```json
[
  {
    "id" : "34b30b63-b89c-4f90-9a4c-436950f50e55",
    "name" : "Keyboard Mechanical",
    "description" : "Switch tactile",
    "createdAt" : "2026-09-09T11:27:50.808570+07:00"
  },
  {
    "id" : "afa47299-383c-4a4f-86b2-9912f4da7cfa",
    "name" : "PostgreSQL + jOOQ Book",
    "description" : "Buku panduan integrasi Vert.x dan jOOQ ORM",
    "createdAt" : "2026-09-09T11:30:28.189465+07:00"
  }
]
```

---

### 3. Menambahkan Item Baru
Menyimpan data item baru ke tabel `items` di PostgreSQL melalui jOOQ ORM.

**Perintah cURL (Windows PowerShell):**
```powershell
curl.exe -s -X POST http://localhost:8080/api/v1/items `
  -H "Content-Type: application/json" `
  -d '{\"name\":\"Laptop MacBook Pro M3\",\"description\":\"Apple Silicon 16GB RAM\"}'
```

**Perintah cURL (Linux / macOS / Git Bash):**
```bash
curl -s -X POST http://localhost:8080/api/v1/items \
  -H "Content-Type: application/json" \
  -d '{"name":"Laptop MacBook Pro M3","description":"Apple Silicon 16GB RAM"}'
```

**Atau menggunakan PowerShell `Invoke-RestMethod`:**
```powershell
$body = @{
  name = "Laptop MacBook Pro M3"
  description = "Apple Silicon 16GB RAM"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/v1/items" -Method Post -ContentType "application/json" -Body $body | ConvertTo-Json
```

**Contoh Response (`201 Created`):**
```json
{
  "id" : "7e5033c4-fce8-48b4-9c87-8df209b55218",
  "name" : "Laptop MacBook Pro M3",
  "description" : "Apple Silicon 16GB RAM",
  "createdAt" : "2026-09-09T11:35:10.512341+07:00"
}
```

---

### 4. Mengambil Item Berdasarkan ID
Mengambil data satu item spesifik menggunakan UUID-nya.

**Perintah cURL:**
```powershell
curl.exe -s http://localhost:8080/api/v1/items/7e5033c4-fce8-48b4-9c87-8df209b55218
```

**Contoh Response (`200 OK`):**
```json
{
  "id" : "7e5033c4-fce8-48b4-9c87-8df209b55218",
  "name" : "Laptop MacBook Pro M3",
  "description" : "Apple Silicon 16GB RAM",
  "createdAt" : "2026-09-09T11:35:10.512341+07:00"
}
```

---

### 5. Menghapus Item
Menghapus item dari database berdasarkan ID.

**Perintah cURL:**
```powershell
curl.exe -s -X DELETE http://localhost:8080/api/v1/items/7e5033c4-fce8-48b4-9c87-8df209b55218
```

**Contoh Response (`200 OK`):**
```json
{
  "message" : "Item berhasil dihapus",
  "id" : "7e5033c4-fce8-48b4-9c87-8df209b55218"
}
```

---

### 6. Pengujian Validasi Error 400 Bad Request
Jika field `name` kosong atau body JSON tidak valid.

**Perintah cURL:**
```powershell
curl.exe -s -i -X POST http://localhost:8080/api/v1/items `
  -H "Content-Type: application/json" `
  -d '{\"description\":\"Item tanpa name\"}'
```

**Contoh Response (`400 Bad Request`):**
```text
HTTP/1.1 400 Bad Request
content-type: application/json
content-length: 59

{"error":"Bad Request","message":"Field 'name' wajib diisi"}
```

---

### 7. Pengujian Endpoint Tidak Ada (404 Not Found)
Jika mengakses URL atau item ID yang tidak terdaftar.

**Perintah cURL:**
```powershell
curl.exe -s -i http://localhost:8080/api/v1/items/id-yang-tidak-ada
```

**Contoh Response (`404 Not Found`):**
```text
HTTP/1.1 404 Not Found
content-type: application/json
content-length: 93

{"error":"Not Found","message":"Item dengan ID 'id-yang-tidak-ada' tidak ditemukan"}
```

---

## 🔍 Verifikasi Langsung ke Database PostgreSQL

Anda dapat memastikan bahwa data benar-benar tersimpan ke PostgreSQL menggunakan tool CLI `psql`:

```powershell
psql -U root -h localhost -p 5432 -d belajar-vertx-db -c "SELECT id, name, description, created_at FROM items;"
```

**Contoh Hasil Query:**
```text
                  id                  |          name          |                description                 |          created_at           
--------------------------------------+------------------------+--------------------------------------------+-------------------------------
 34b30b63-b89c-4f90-9a4c-436950f50e55 | Keyboard Mechanical    | Switch tactile                             | 2026-09-09 11:27:50.80857+07
 afa47299-383c-4a4f-86b2-9912f4da7cfa | PostgreSQL + jOOQ Book | Buku panduan integrasi Vert.x dan jOOQ ORM | 2026-09-09 11:30:28.189465+07
(2 rows)
```

### Memeriksa Riwayat Migrasi Liquibase (`databasechangelog`)

```powershell
psql -U root -h localhost -p 5432 -d belajar-vertx-db -c "SELECT id, author, filename, dateexecuted, exectype FROM databasechangelog;"
```

**Contoh Hasil:**
```text
           id           | author |                     filename                     |        dateexecuted        | exectype 
------------------------+--------+--------------------------------------------------+----------------------------+----------
 001-create-items-table | teten  | db/changelog/changes/001-create-items-table.sql  | 2026-09-09 13:02:03.647604 | MARK_RAN
(1 row)
```
