# SQL Server 2022 Setup Guide

## Overview
This guide details the setup of Microsoft SQL Server 2022 Developer Edition for a Spring Boot application.

## Table of Contents
- [Prerequisites](#prerequisites)
- [Installation Steps](#installation-steps)
- [Database Configuration](#database-configuration)
- [Security Notes](#security-notes)
- [Troubleshooting](#troubleshooting)

## Prerequisites
* Windows PC
* Administrative privileges
* Internet connection

## Installation Steps

### 1. Download and Install SQL Server
1. Visit [Microsoft SQL Server Downloads](https://www.microsoft.com/en-us/sql-server/sql-server-downloads)
2. Download SQL Server 2022 Developer Edition
3. Run installer and select "Basic" installation
4. Follow installation wizard prompts

### 2. Configure SQL Server Network
1. Open SQL Server Configuration Manager
2. Navigate to: SQL Server Network Configuration > Protocols for MSSQLSERVER
3. Enable TCP/IP
4. Set TCP Port to 1433

### 3.0 Database Setup
Execute in SQL Server Management Studio:

```sql
CREATE DATABASE transactions_db;
GO

CREATE LOGIN sa WITH PASSWORD = 'Pass@123';
GO

USE master;
GO
EXEC xp_instance_regwrite 
    N'HKEY_LOCAL_MACHINE', 
    N'Software\Microsoft\MSSQLServer\MSSQLServer',
    N'LoginMode', 
    REG_DWORD,
    2;
GO

ALTER SERVER ROLE sysadmin ADD MEMBER [sa];
GO
```
### 3.1 Tables Setup
Execute the following SQL commands to generate the required tables based on the schemas below:
```sql
USE transactions_db;
GO

-- Create accounts table first (since it's referenced by transactions)
CREATE TABLE accounts (
    account_id VARCHAR(255) PRIMARY KEY,
    balance DECIMAL(10,2) NOT NULL,
    created_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    version BIGINT NOT NULL DEFAULT 0,
    last_modified_at DATETIMEOFFSET NULL,
);
GO

-- Create ENUM tables for TransactionType and TransactionStatus
CREATE TABLE TransactionType (
    type_name VARCHAR(50) PRIMARY KEY
);
GO

CREATE TABLE TransactionStatus (
    status_name VARCHAR(50) PRIMARY KEY
);
GO

-- Insert enum values
INSERT INTO TransactionType (type_name) VALUES ('DEPOSIT'), ('WITHDRAWAL');
GO

INSERT INTO TransactionStatus (status_name) VALUES ('COMPLETED'), ('FAILED');
GO

-- Create transactions table
CREATE TABLE transactions (
    transaction_id CHAR(36) PRIMARY KEY,  -- For UUID
    account_id VARCHAR(255) NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    timestamp DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    status VARCHAR(50) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT FK_transactions_account 
        FOREIGN KEY (account_id) REFERENCES accounts(account_id),
    CONSTRAINT FK_transactions_type 
        FOREIGN KEY (transaction_type) REFERENCES TransactionType(type_name),
    CONSTRAINT FK_transactions_status 
        FOREIGN KEY (status) REFERENCES TransactionStatus(status_name),
    CONSTRAINT CHK_amount 
        CHECK (amount <= 10000.00)
);
GO

-- Create indexes
CREATE INDEX IX_transactions_account_id ON transactions(account_id);
CREATE INDEX IX_accounts_created_at ON accounts(created_at);
CREATE INDEX IX_accounts_last_modified_at ON accounts(last_modified_at);
GO

-- Test data insertion example:
-- Insert an account
INSERT INTO accounts (account_id, balance, created_at, last_modified_at) 
VALUES ('ACC123', 1000.00, SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET());
GO

-- Insert a transaction
INSERT INTO transactions (
    transaction_id, 
    account_id, 
    transaction_type, 
    amount, 
    timestamp, 
    status
) 
VALUES (
    NEWID(), 
    'ACC123', 
    'DEPOSIT', 
    500.00, 
    SYSDATETIMEOFFSET(), 
    'COMPLETED'
);
GO

-- Verify tables creation:
SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE';
GO
```
### 4. Current spring boot props used in this demo server
```properties
spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=transactions_db;encrypt=true;trustServerCertificate=true
spring.datasource.username=sa
spring.datasource.password=1
```

## Security Notes
⚠️ **Important:**
- These credentials are for development only
- Never used in production
- Change default passwords
- Avoid using 'sa' account
- Enable Windows Authentication when possible

## Troubleshooting

### Common Issues
1. Connection Failed
    - Verify SQL Server service is running
    - Check Windows Firewall
    - Confirm TCP/IP protocol status

2. Port Issues
    - Ensure port 1433 is not blocked
    - Check for port conflicts

### Verification Steps
1. Open SQL Server Management Studio
2. Connect using:
    - Server: localhost
    - Authentication: SQL Server Authentication
    - Username: sa
    - Password: 1
3. If above fails, attempt logging in with
   - Server: localhost
   - Authentication: Windows Authentication
   - Encryption: Optional
4. Make sure to enable windows and SQL server auth mode like shown below:
![SQl login failures from spring boot server](/src/main/resources/readme/sql-login.png)
5. Restart the SQL server instance on Windows services or by restarting your pc.

## Additional Resources
- [SQL Server Documentation](https://docs.microsoft.com/en-us/sql/sql-server/)
- [Spring Boot SQL Server Guide](https://spring.io/guides)
