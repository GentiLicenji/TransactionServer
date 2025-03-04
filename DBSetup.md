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
Execute in SQL cmd line or SQL Server Management Studio:

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
-- Create Schema (optional but recommended)
CREATE SCHEMA transaction_system;
GO

-- Account Table
CREATE TABLE transaction_system.accounts (
                                            account_id BIGINT IDENTITY(1,1) PRIMARY KEY,
                                            account_number VARCHAR(255) NOT NULL,
                                            first_name NVARCHAR(255) NOT NULL,
                                            last_name NVARCHAR(255) NOT NULL,
                                            balance DECIMAL(19,2) NOT NULL,
                                            created_at DATETIMEOFFSET NOT NULL DEFAULT SYSUTCDATETIME(),
                                            version BIGINT DEFAULT 0,
                                            last_modified_at DATETIMEOFFSET NULL,
                                            CONSTRAINT UQ_Account_Number UNIQUE (account_number),
                                            CONSTRAINT CHK_Account_Balance CHECK (balance >= 0)
);
GO

-- Transaction Table
CREATE TABLE transaction_system.transactions (
                                                transaction_id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWSEQUENTIALID(),
                                                account_id BIGINT NOT NULL,
                                                transaction_type VARCHAR(50) NOT NULL,
                                                amount DECIMAL(19,2) NOT NULL,
   [timestamp] DATETIMEOFFSET NOT NULL DEFAULT SYSUTCDATETIME(),
   status VARCHAR(50) NOT NULL,
   version BIGINT DEFAULT 0,
   CONSTRAINT FK_Transaction_Account FOREIGN KEY (account_id)
      REFERENCES transaction_system.accounts(account_id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
   CONSTRAINT CHK_Transaction_Amount CHECK (amount <= 10000.00),
   CONSTRAINT CHK_Transaction_Type CHECK (transaction_type IN ('DEPOSIT', 'WITHDRAWAL')),
   CONSTRAINT CHK_Transaction_Status CHECK (status IN ('COMPLETED', 'FAILED'))
   );
GO

-- Indexes
CREATE NONCLUSTERED INDEX IX_Account_Number 
ON transaction_system.accounts(account_number);

CREATE NONCLUSTERED INDEX IX_Transaction_AccountId 
ON transaction_system.transactions(account_id)
INCLUDE (transaction_type, amount, [timestamp], status);

CREATE NONCLUSTERED INDEX IX_Transaction_Timestamp 
ON transaction_system.transactions([timestamp]);

CREATE NONCLUSTERED INDEX IX_Transaction_Type_Status 
ON transaction_system.transactions(transaction_type, status)
INCLUDE (account_id, amount, [timestamp]);

-- Verify tables creation:
SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE';

```

Optimize common queries:
```sql
-- For countRecentTransactions
CREATE NONCLUSTERED INDEX IX_Transaction_AccountId_Timestamp
ON transaction_system.transactions(account_id, [timestamp])
INCLUDE (transaction_type, status);

-- For findByAccountNumber
CREATE NONCLUSTERED INDEX IX_Account_Number
ON transaction_system.accounts(account_number)
INCLUDE (first_name, last_name, balance, created_at);
```
Create filtered indexes for common patterns:
```sql
-- Query recent transactions (last 30 days)
CREATE NONCLUSTERED INDEX IX_Transaction_Recent
ON transaction_system.transactions(account_id, [timestamp])
INCLUDE (transaction_type, status)
WHERE [timestamp] >= DATEADD(DAY, -30, SYSUTCDATETIME());

-- Query completed transactions
CREATE NONCLUSTERED INDEX IX_Transaction_Completed
ON transaction_system.transactions(account_id, [timestamp])
INCLUDE (transaction_type)
WHERE status = 'COMPLETED';
```
Insert an account record if you want to test the server using Postman Test Suite:
<br/>The structure breakdown:
* GB: Country code (Great Britain)
* XX: Check digits (automatically calculated)
* NWBK: Bank identifier (in this case, NatWest Bank)
* 601613: Sort code
* 31926819: Account number
```sql
INSERT INTO transaction_system.accounts (account_number, first_name, last_name, balance, created_at, version)
VALUES ('GB29NWBK60161331926819', 'John', 'Doe', 1000.00, SYSUTCDATETIME(), 0);
INSERT INTO transaction_system.accounts (account_number, first_name, last_name, balance, created_at, version)
VALUES ('GB94NWBK60161331926822', 'John', 'Snow', 1000.00, SYSUTCDATETIME(), 0);
INSERT INTO transaction_system.accounts (account_number, first_name, last_name, balance, created_at, version)
VALUES ('GB82NWBK60161331926820', 'Bob', 'Builder', 1000.00, SYSUTCDATETIME(), 0);
```
### 3.2 Advanced Implementation
I could have implemented the transaction creation business logic within the database using stored procedures. 
<br/>I chose to do it within the application's service layer since there were no strict performance requirements.
<br/>Here's a sample of that implementation below:
```sql
CREATE OR ALTER PROCEDURE transaction_system.sp_CreateTransaction
    @AccountNumber VARCHAR(255),
    @Amount DECIMAL(19,2),
    @TransactionType VARCHAR(50),
    @TransactionId UNIQUEIDENTIFIER OUTPUT  -- This is an output parameter
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;  -- Ensures transaction safety
    
    BEGIN TRY
        BEGIN TRANSACTION;
            
        -- First check if account exists and get its ID
        DECLARE @AccountId BIGINT;
        SELECT @AccountId = account_id 
        FROM transaction_system.accounts 
        WHERE account_number = @AccountNumber;
        
        IF @AccountId IS NULL
            THROW 50000, 'Account not found', 1;
            
        -- Create new transaction
        SET @TransactionId = NEWID();
        
        INSERT INTO transaction_system.transactions (
            transaction_id,
            account_id,
            amount,
            transaction_type,
            status
        ) VALUES (
            @TransactionId,
            @AccountId,
            @Amount,
            @TransactionType,
            'COMPLETED'
        );
        
        -- Update account balance
        UPDATE transaction_system.accounts
        SET balance = CASE 
            WHEN @TransactionType = 'DEPOSIT' THEN balance + @Amount
            WHEN @TransactionType = 'WITHDRAWAL' THEN balance - @Amount
        END
        WHERE account_id = @AccountId;
        
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0
            ROLLBACK TRANSACTION;
            
        THROW;  -- Re-throw the error
    END CATCH;
END;

-- How to use it in MSSQL:
DECLARE @NewTransactionId UNIQUEIDENTIFIER;
EXEC transaction_system.sp_CreateTransaction 
    @AccountNumber = '12345',
    @Amount = 100.00,
    @TransactionType = 'DEPOSIT',
    @TransactionId = @NewTransactionId OUTPUT;
```
In my spring boot app tran repository, I would have called:
```java
    @Procedure(name = "sp_CreateTransaction")
UUID createTransaction(
                @Param("AccountNumber") String accountNumber,
                @Param("Amount") Double amount,
                @Param("TransactionType") String transactionType
        );
```

### 4. Current spring boot props used in this demo server
```properties
spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=transactions_db;encrypt=true;trustServerCertificate=true
spring.datasource.username=sa
spring.datasource.password=Pass@123
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
    - Password: Pass@123
3. If above fails, attempt logging in with
   - Server: localhost
   - Authentication: Windows Authentication
   - Encryption: Optional
4. Make sure to enable windows and SQL server auth mode like shown below:
![SQl login failures from spring boot server](/src/main/resources/readme/sql-login.png)
5. Restart the SQL server instance on Windows services or by restarting your PC.

## Additional Resources
- [SQL Server Documentation](https://docs.microsoft.com/en-us/sql/sql-server/)
- [Spring Boot SQL Server Guide](https://spring.io/guides)
