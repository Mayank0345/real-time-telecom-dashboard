# Real-Time Telecom Data Dashboard

A beginner-friendly Java 17, Spring Boot, MySQL, and Chart.js project. It generates sample subscribers, network usage, and outages; saves them to MySQL; and serves a small dashboard backed by REST APIs.

## What is included

- REST endpoints for dashboard statistics and regions
- JPA entities for subscribers, regions, usage data, and network outages
- Starter sample data plus a new usage record every 10 seconds
- Subscriber and active-user statistics, plus total data usage in GB
- Daily, monthly, and regional usage charts, with per-region network outage status
- Active/inactive subscribers, average call duration, SMS totals, and recent usage records
- Date and region filters; dashboard data refreshes every 10 seconds

## Prerequisites

- Java 17 or newer
- Maven 3.8+
- MySQL 8+

## Run locally

1. Create the database tables using the included schema in MySQL Workbench or the MySQL command-line client:

   ```sql
   SOURCE "C:/path/to/Real Time Telecom Data Dashboard/database/telecom_dashboard.sql";
   ```

   The script creates the `telecom_dashboard` database and its `regions`, `subscribers`, `usage_data`, and `network_outages` tables. The application adds sample rows on first startup.

2. Create a MySQL user for the application:

   ```sql
   CREATE USER 'telecom_user'@'localhost' IDENTIFIED BY 'change_me';
   GRANT ALL PRIVILEGES ON telecom_dashboard.* TO 'telecom_user'@'localhost';
   ```

3. Set connection details if yours differ from the defaults:

   ```powershell
   $env:DB_URL = "jdbc:mysql://localhost:3306/telecom_dashboard"
   $env:DB_USERNAME = "telecom_user"
   $env:DB_PASSWORD = "change_me"
   ```

   The datasource settings are read from these environment variables, so passwords do not need to be stored in the project. SQL statements are logged by default for learning; set `$env:JPA_SHOW_SQL = "false"` to turn that off.

4. Start the application from the project root:

   ```powershell
   mvn -f backend\pom.xml spring-boot:run
   ```

5. Open [http://localhost:8080](http://localhost:8080). The dashboard is also available at [http://localhost:8080/](http://localhost:8080/).

On its first run, the application creates the tables and seeds regions, active and inactive subscribers, a week of sample usage, and an example outage. New usage is generated only for active subscribers. Hibernate updates the schema on later starts. Do not use `ddl-auto=update` as a production migration strategy.

## REST API

- `GET /api/dashboard` — summary, usage trend, regional usage, and outages
- `GET /api/dashboard?from=2025-01-01&to=2025-01-07&regionId=1` — apply inclusive date and optional region filters
- `GET /api/dashboard/stats` — subscriber, usage, call-duration, and outage summary
- `GET /api/subscribers?regionId=1` — subscriber list, optionally filtered by region
- `GET /api/usage?from=2025-01-01&to=2025-01-07&regionId=1` — usage records for a date range and optional region
- `GET /api/usage/monthly?regionId=1` — rolling 12-month usage trend, optionally filtered by region
- `GET /api/usage/recent?from=2025-01-01&to=2025-01-07&regionId=1` — latest 20 usage records in the date range
- `GET /api/usage/region` and `GET /api/usage/daily` — regional and daily usage aggregates
- `GET /api/outages?from=2025-01-01&to=2025-01-07&regionId=1` — outages in the selected range and optional region
- `GET /api/regions` — available regions for the filter

Dates use ISO format (`YYYY-MM-DD`). The default dashboard range is the last seven calendar days, including today. `totalUsageMb` is usage in that selected range; subscriber count is the current subscriber base (limited by region when selected). Outages shown are the most recently started outages in the selected range.

`GET /api/dashboard/stats` returns `totalSubscribers`, `activeSubscribers`, `totalDataUsage`, `averageCallDuration`, `totalSmsCount`, `totalOutages`, and `activeOutages`. Usage and total outage metrics use the selected date range; subscriber counts and active outage counts reflect the current state (optionally limited by region). Date-filtered endpoints default to the last seven days and accept ranges up to one year.

The browser dashboard calls the stats, daily, monthly, regional, outage, and recent-usage endpoints in parallel. Date-range endpoints use the selected dates and optional region; the monthly chart always covers the rolling last 12 months and honors the selected region. Successful JSON responses update the summary cards, charts, and tables every 10 seconds.

## Tests and verification

Run the automated tests with:

```powershell
mvn -f backend\pom.xml test
```

The test suite uses an in-memory H2 database in MySQL compatibility mode to verify JPA mappings, repository queries, and every query in `database/analytics.sql`. Mocked tests cover empty usage/subscribers, unknown regions, generator behavior, REST JSON, invalid date ranges, empty API results, and the API's database-unavailable response. H2 verifies application/database integration without requiring a local MySQL server; it does not replace a live MySQL connection test. Use the following smoke check with your configured MySQL instance to verify the actual MySQL connection:

```powershell
mvn -f backend\pom.xml spring-boot:run
```

In another PowerShell window:

```powershell
Invoke-RestMethod http://localhost:8080/api/dashboard/stats
Invoke-RestMethod http://localhost:8080/api/usage/recent
```

Then confirm new rows are appearing in MySQL:

```sql
USE telecom_dashboard;
SELECT COUNT(*) FROM usage_data;
SELECT COUNT(*) FROM network_outages;
```

## SQL analytics

Run the sample MySQL analytics queries after creating the schema:

```sql
SOURCE "C:/path/to/Real Time Telecom Data Dashboard/database/analytics.sql";
```

The queries calculate regional and daily data usage, average usage, the most active region, subscriber counts by region and status, and outage counts and average resolved duration. Regional usage joins `usage_data` to `subscribers` and `regions`, matching the schema's normalized foreign keys.

## Project layout

```text
backend/
  pom.xml
  src/
    main/java/com/telecom/dashboard/
      controller/    REST endpoints and API error responses
      dto/           Dashboard response data
      model/         JPA entities mapped to the MySQL schema
      repository/    Spring Data repositories
      service/       Dashboard queries and simulated data generation
    main/resources/
      application.properties
    test/java/       Automated tests
frontend/
  index.html       Dashboard page
  style.css        Dashboard styles
  script.js        REST API integration and charts
database/
  telecom_dashboard.sql  MySQL database and table creation script
  analytics.sql          Example telecom usage and outage analytics queries
screenshots/
  Add dashboard screenshots here before publishing to GitHub
```

Maven packages the files from `frontend/` into Spring Boot's `static/` classpath at build time, so the dashboard continues to be served from the same application.

## Deployment preparation

The repository includes a [Dockerfile](./Dockerfile) for the Spring Boot API and [Vercel configuration](./frontend/vercel.json) for the standalone static frontend. Local development continues to use MySQL. The backend also supports PostgreSQL so a free Render web service can use a Neon Free database.

### 1. Provision PostgreSQL on Neon

Create a PostgreSQL project on Neon's **Free** plan. Neon currently advertises a $0 plan with per-project compute and storage quotas and no credit card requirement; check the current pricing and limits before use. From the Neon dashboard, get the connection details and keep the password private. Never commit database credentials to GitHub.

### 2. Deploy the backend to Render

Create a Render **Web Service** from the GitHub repository and select **Docker** as the runtime. Use the repository root as the Docker context and `Dockerfile` as the Dockerfile path. Select the **Free** instance type if available; do not select a paid plan. Render supplies `PORT`; the container uses it automatically. Set these environment variables in Render:

| Variable | Value |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://HOST/DATABASE?sslmode=require` using Neon's connection host and database name |
| `DB_DRIVER` | `org.postgresql.Driver` |
| `DB_USERNAME` | Neon PostgreSQL username |
| `DB_PASSWORD` | Neon PostgreSQL password (secret) |
| `JPA_DDL_AUTO` | `update` for the first deployment so Hibernate can create the tables |
| `JPA_SHOW_SQL` | `false` |
| `CORS_ALLOWED_ORIGINS` | Additional allowed origins, comma-separated (for example, localhost or specific Vercel preview URLs). The production dashboard origin `https://real-time-telecom-dashboard.vercel.app` is included by the backend. |

Set Render's health check path to `/api/regions`. After deploy, confirm `https://YOUR-API.onrender.com/api/dashboard/stats` returns JSON. Free web services may sleep when idle, so the first request after inactivity can take a while. Neon Free has usage and storage limits; confirm the current terms and monitor usage.

### 3. Deploy the frontend to Vercel

Create a Vercel project from the same GitHub repository and set its **Root Directory** to `frontend`. It is a static HTML project; use `index.html` as the entry point and the root (`.`) as the output directory. `frontend/config.js` uses the Render API outside localhost and keeps local development same-origin.

The backend allows the production dashboard origin `https://real-time-telecom-dashboard.vercel.app` alongside the origins configured by `CORS_ALLOWED_ORIGINS`. For Vercel preview deployments, add the specific preview origin(s) you intend to use to that environment variable and redeploy the backend. Do not use wildcard origins for a public production API. Use Vercel's Hobby plan only if it is available to your account at no charge.

### 4. Verify the deployed site

Open the Vercel URL, confirm the dashboard loads, then check browser developer tools for successful `/api/regions`, `/api/dashboard/stats`, `/api/usage/daily`, `/api/usage/monthly`, `/api/usage/region`, `/api/usage/recent`, and `/api/outages` requests. On the backend, check Render logs and confirm `usage_data` continues to grow; the simulator writes one new record every 10 seconds.
