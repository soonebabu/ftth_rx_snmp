# FTTH CPE RX Level ETL

## Overview
This project implements a high-performance ETL (Extract, Transform, Load) system for retrieving and storing **RX (Receive) levels** of FTTH (Fiber to the Home) CPEs (Customer Premises Equipment) from Huawei and ZTE OLTs. The system queries OLT devices using **SNMP**, retrieves ONU serial numbers and associated data, and stores the information in a **MySQL database**.  

The solution is optimized for **high concurrency**, reducing the runtime from hours (using PHP) to just **2 minutes** for millions of CPEs.

## Key Features
- Retrieves **ONU serial numbers** from Huawei and ZTE OLTs via SNMP.
- Collects additional metadata: OLT, FDC information, RX power, distance, OLT RX power, and temperature.
- Supports **5 types of OLTs** per vendor and **6 geographical regions**, resulting in 30 concurrent Docker images.
- Each Docker image executes **100 Java threads** to extract data in parallel.
- Handles **5,22,718 unique CPEs** efficiently.
- Stores data in MySQL with **bulk insert and update** operations for performance.
- Built-in logging for **error handling and monitoring**.

## Architecture
               +----------------------+
               |     OLT Device       |
               | Huawei / ZTE         |
               | SNMP API             |
               +----------+-----------+
                          |
                          v
                 +-----------------+
                 | SNMP Query App  |
                 |  (Java + Threads)|
                 +--------+--------+
                          |
    +-----------------------------------------+
    |  DAO Layer (Database Access Object)     |
    |  - Fetch Node/NodeType info             |
    |  - Save SNMP data to MySQL              |
    +-----------------------------------------+
                          |
                          v
                 +-----------------+
                 | MySQL Database  |
                 | onuserial table |
                 +-----------------+




## Workflow
1. **Retrieve ONU Serial Numbers**
   - SNMP query sent to each OLT to fetch ONU serials.
   - Additional metadata (OLT, FDC, node type, region) is stored in MySQL.

2. **Fetch RX Levels**
   - Using the serial numbers, another SNMP query retrieves RX power, distance, OLT RX power, and temperature.
   - SNMP OIDs vary by OLT type and vendor.

3. **Parallel Processing**
   - 30 Docker images executed concurrently (5 OLT types × 6 regions).
   - Each Docker image runs 100 Java threads.
   - Reduces runtime from several hours (PHP) to **~2 minutes**.

4. **Data Storage**
   - Retrieved data is saved into MySQL in bulk.
   - Tables involved: `onuserial`, `onuserialraw`, `pingstatus`, `oidnotfound`.
   - Stored procedures handle batch insert and synchronization.

## Technology Stack
- **Language:** Java (SNMP4J library)
- **Database:** MySQL
- **Concurrency:** Java ExecutorService, multi-threading
- **Containers:** Docker (30 images for parallel extraction)
- **Vendors Supported:** Huawei, ZTE
- **Geographical Regions:** 6 regions
- **SNMP Version:** SNMP v2c
- **Logging:** SLF4J

## Installation
1. Clone the repository:
```bash
git clone https://github.com/soonebabu/ftth-cpe-rx-etl.git
cd ftth-cpe-rx-etl

2. Configure MySQL connection in DataSourceSingleton.java.

3. Build Docker image:
docker build -t ftth-etl-image .

4. Run Docker containers (one per OLT type × region):
docker run -d --name ftth-etl-container ftth-etl-image java -jar etl-app.jar

5.Monitor logs:
docker logs -f ftth-etl-container
