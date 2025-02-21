# TransactionServer

## Project overview

## Setup instructions

### DB Setup
Please see the [DB Setup README](./DBSetup.md) on setup details.

## Automated API changes - Swagger Codegen
All api changes and definitions are captured through [OpenAPI Specification (OAS) standard](https://swagger.io/specification/).
<br/>Transaction Server utilizes [swagger codegen library](https://github.com/swagger-api/swagger-codegen) to generate code based on this spec.
<br/>This generator can be used to create controller and model classes, which reduce the code changes required when changing a Rest Api spec.
<br/>The overall project structure shows which components will be auto-generated:
```
|-- src
|   `-- main
|       |-- java
|       |   `-- com.sisal.transaction.server
|       |               |-- controller (generated with custom swagger codegen - contains spring & springfox annotations)
|       |               |-- filter
|       |               |-- delegate
|       |               |-- service
|       |               |-- config
|       |               |-- exception
|       |               |-- model (generated with custom swagger codegen)
|       |               `-- util
|       `-- resources
```

***NOTE***
<br/> I have extensive experience using Swagger (now OpenAPI).
<br/> During my time at ScotiaBank, I served as the sole contributor to an open-source extension of Swagger Codegen.
<br/> My work involved creating custom mustache templates to support multiple use cases across North and South America, as well as enhancing internal libraries built on top of the Spring Framework.
<br/>For this project, I'm leveraging my own private, customized solution I developed based on Swagger Codegen to ensure robust integration and functionality.
<br/>This will ensure fast and easy API updates to the codebase straight from the spec changes.

## Design Decisions and Best Practices
### Architecture Overview
Good design patterns that are followed:
* Model View Controller pattern (Provides intuitive method based annotation to define a url mapping)
* Dependency Injection pattern. (Promotes decoupling with singletons and bean auto-wiring )
* Front Controller design pattern (Provides centralized logging/authentication/validation/exception-handling)

### Exception Handling and Validation
I am a firm believer in pushing error handling and validation on the outside boundaries of the application.
<br/> For this reason I do not apply null checks, and try catch blocks within the application logic layers.
<br/> Of course, there are exceptions to be made on exception handling. Pun intended. 😄
<br/> I let validation be applied by the swagger spec itself through jackson annotation.
<br/> Some rare business cases might call
for creating a custom validator for complex business rules applied on the spring filter chain layer.

### Object Mapping 
MapStruct vs Lombok vs Fluid API setters
<br/> I would try to avoid lombok because annotation processing needs to be enabled in certain IDEs and does not improve readability by using the classic setter getters when called.
<br/> I would opt out for the Fluid api setters using pure java code. It is also the common choice when generating java models in Swagger/OpenAPI.
<br/> I would use mapstruct for complex model mappings that require a lot of code manipulation as it simplifies the whole process and saves time.

### Business Logic Implementation Ideas

* Transaction count limit.
<br/>I implemented this transaction management requirement by using spring data jpa queries. 
<br/>This is great for a single instance that requires exact transaction count,
but can be computationally intensive and can cause transaction locking in high concurrency scenarios.
<br/> Transaction and account locking are necessary to prevent race conditions during updates.

* <br/>The Redis Cluster Cache approach is the best solution for rate limiting in because:
- It's more accurate in a distributed environment
- It automatically handles the sliding window
- It's more efficient than database queries
- It automatically cleans up after the minute expires

* Maximum transaction amount (Validation is applied )
* Minimum account balance
* Avoid race conditions by locking account row while checking balance or transaction count.

## Database Choice
### Preface
During my 8 years of experience in the fintech space, relational databases were the mandatory choice.
<br/>Here are some reasons:
* ACID Transactions (ensures data integrity)
* Enforce data constraints (ability to combine validation with the business layer by using transaction-bound queries or checks)
* SQL Queries are a powerful tool for Reporting and Auditing
* Maturity and Ecosystem:
    * PostgreSQL and MySQL have robust support with Spring Data and JPA/Hibernate, making them straightforward to set up and use.
    * They offer mature tooling for migrations (Flyway or Liquibase), monitoring, backups, and replication.

When deciding database solutions, there are a few factors that I take into account:
* Client-specific requirements (In this case, I don't have any functional requirements outlined on scale/performance/budget/analytics)
* Adoption and maintenance cost (Critical in most companies)
* Team skill-set (Team skill and familiarity play a big role especially in smaller companies/ startups)
* Company adoption (Usually there are one or two main solutions that are implemented company-wide)
* Industry adoption (Typically I try to select industry-wide adopted tools that have a proven track record along years)

### PostgreSQL Vs MySQL Vs Microsoft SQL Server Vs Oracle Database
(Source:ChatGPT - O1)
* PostgreSQL:
  * Rich Feature Set and Standards Compliance: Adheres closely to SQL standards, offering advanced SQL features such as window functions, CTEs, and JSONB.
  * Ideal for Complex Queries and Analytics: Superior query optimizer and support for complex operations make it well-suited for transactional and analytical workloads.
* MySQL:
  * Simpler Read-Heavy Workloads: Known for good performance on straightforward read-intensive applications.
  * Widespread Adoption and Tooling: Has a massive user base, extensive documentation, and a variety of external tools for setup, maintenance, and scaling.
* Oracle Database or Microsoft SQL Server:
  * Feature-rich and great performance, but typically come with steep licensing costs and, in many cases, higher operational overhead.
  * They’re often used in large enterprises that need specific features or already have an ecosystem built around these databases and can justify the associated cost.
* SQLite:
  * Extremely lightweight but not ideal for concurrent usage at scale or where advanced SQL queries and high transaction throughput are needed.
  * Typically used for prototyping, small-scale applications, or embedded scenarios.

### DB final decision
I will be using a Microsoft SQL database for main functionality due to the familiarity with the tool and the requirements presented.
<br/> I will be using an H2 database for integration and unit testing due to ease of use.
<br/> Below is the project setup for the databases used:
```
src/
├── main/
│   ├── java/
│   └── resources/
│       └── application.properties  (SQL Server config)
└── test/
    ├── java/
    └── resources/
        └── application-test.properties  (H2 config)
```




## Third-party library usage.

### MapStruct vs

### Spring boot versioning decision
<br/> Due to the project requirement to use java 8 or above, 
<br/> I was constrained to use an older version of Spring Boot (Spring Boot 2.7.x will be supported until August 2024).
<br/>Despite development and setup being easier with newer spring boot versions by using Spring Initializr I chose to go the harder path.
Also in my personal experience financial institutions are hard to adapt to newer technological advancement.

### SpringFox vs SpringDoc
<br/> SpringDoc is the new team, where old members from SpringFox moved to. 
<br/> There are similar political reasons with what happened with Swagger and OpenAPI standard.
<br/> Due to the project requirement to use swagger I was constrained to use the old version of swagger 2.0.

<p> Here's some more details on the Timeline & Evolution:
<br/> (Source:Claude 3.5 Sonnet)

* 2010-2011: Swagger Created
  - Originally developed by Wordnik
  - First major API documentation tool
  - Became very popular in API development

* 2015: SmartBear Acquisition
  - SmartBear acquired Swagger
  - Continued development under SmartBear

* 2016: OpenAPI Initiative (OAI)
  - Swagger Specification was donated to Linux Foundation
  - Renamed to OpenAPI Specification (OAS)
  - Became vendor-neutral standard

* Current State:
  - Swagger = SmartBear's toolset (SwaggerUI, Swagger Editor, etc.)
  - OpenAPI = The specification standard
  - OpenAPI 3.0+ = Current specification version

* Use SpringDoc for:
  - New projects
  - Spring Boot 2.6+
  - Need for OpenAPI 3.0
  - Active maintenance

* Use Springfox only if:
  - Legacy projects
  - Specific requirement for Swagger 2.0
  - Cannot upgrade existing codebase

## Details on the implementation of the authentication mechanism

[//]: # (TODO: Create a custom OncePerRequestFilter in the filter layer)
[//]: # (TODO: More work needed here)
sample api request for authentication
http POST :8080/api/transactions
X-API-Key: kjasdhfjashdf
X-Timestamp:1697040568000
X-HMAC-Signature: kjashdfjkhasdfjhkjlasdfkjasdf
Content-Type:application/json

## Contributing
This is a product of © Sisal.
<br/>Developers:
Gentian Licenji

## License
No Licenses.

## Project status
Deadline - Feb 21st,2025.
<br/> Project is on time as scheduled.

### References and other resources <a name="references"></a>
* [Swagger codegen](https://swagger.io/docs/open-source-tools/swagger-codegen/)
* [Apache Maven](https://maven.apache.org/)
* [SpringFox](https://springfox.github.io/springfox/docs/current/)
* [Spring framework](https://spring.io/projects/spring-framework)
* [Spring Boot](https://spring.io/projects/spring-boot)
* [Log4J](https://logging.apache.org/log4j/1.2/manual.html)