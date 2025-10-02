# Lambda Function Code Reference

This document contains all the code needed for Step 3 and Step 4 of the lab.

## Table of Contents
- [Step 3: Lambda Function Implementation](#step-3-lambda-function-implementation)
  - [Project Setup](#project-setup)
  - [ExportJobMessage.java](#exportjobmessagejava)
  - [ExportService.java](#exportservicejava)
  - [ExportHandler.java (Step 3)](#exporthandlerjava-step-3)
  - [pom.xml](#pomxml)
  - [Building and Deployment](#building-and-deployment)
  - [Testing](#testing)
- [Step 4: DynamoDB Integration](#step-4-dynamodb-integration)
  - [Additional Imports](#additional-imports)
  - [Additional Fields and Constants](#additional-fields-and-constants)
  - [Modified Constructor](#modified-constructor)
  - [Modified processMessage Method](#modified-processmessage-method)
  - [New updateJobStatus Method](#new-updatejobstatus-method)

---

# Step 3: Lambda Function Implementation

## Project Setup

### Directory Structure

```
export-lambda-function/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── ie/
                └── ul/
                    └── csis/
                        └── lambda/
                            ├── ExportHandler.java
                            ├── model/
                            │   └── ExportJobMessage.java
                            └── service/
                                └── ExportService.java
```

---

## ExportJobMessage.java

**Location:** `src/main/java/ie/ul/csis/lambda/model/ExportJobMessage.java`

**Purpose:** Data model for parsing SQS message JSON

```java
package ie.ul.csis.lambda.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExportJobMessage {

    @JsonProperty("jobId")
    private String jobId;

    @JsonProperty("taskType")
    private String taskType;

    @JsonProperty("timestamp")
    private String timestamp;

    public ExportJobMessage() {}

    public ExportJobMessage(String jobId, String taskType, String timestamp) {
        this.jobId = jobId;
        this.taskType = taskType;
        this.timestamp = timestamp;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ExportJobMessage{" +
                "jobId='" + jobId + '\'' +
                ", taskType='" + taskType + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
```

**Key Points:**
- `@JsonProperty` annotations ensure correct JSON field mapping
- No-argument constructor required for Jackson deserialization
- Must match Step 2 message format exactly

**Expected JSON format:**
```json
{
  "jobId": "123e4567-e89b-12d3-a456-426614174000",
  "taskType": "export",
  "timestamp": "2025-09-30T10:30:00Z"
}
```

---

## ExportService.java

**Location:** `src/main/java/ie/ul/csis/lambda/service/ExportService.java`

**Purpose:** Business logic for processing export tasks

```java
package ie.ul.csis.lambda.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);

    public String processExportTask(String jobId, String taskType) {
        logger.info("Starting export task processing - JobId: {}, TaskType: {}", jobId, taskType);

        try {
            // Simulate long-running task - the original 10-second processing from Spring Boot
            logger.info("Executing export task... JobId: {}", jobId);
            Thread.sleep(10000); // 10 seconds to simulate data export

            logger.info("Export task completed - JobId: {}", jobId);
            return "Export completed successfully for job: " + jobId;

        } catch (InterruptedException e) {
            logger.error("Export task interrupted - JobId: {}, Error: {}", jobId, e.getMessage());
            Thread.currentThread().interrupt();
            throw new RuntimeException("Export task interrupted for job: " + jobId, e);
        } catch (Exception e) {
            logger.error("Export task execution failed - JobId: {}, Error: {}", jobId, e.getMessage());
            throw new RuntimeException("Export task failed for job: " + jobId, e);
        }
    }

    public boolean validateTaskParameters(String jobId, String taskType) {
        if (jobId == null || jobId.trim().isEmpty()) {
            logger.error("Invalid JobId: {}", jobId);
            return false;
        }

        if (taskType == null || taskType.trim().isEmpty()) {
            logger.error("Invalid TaskType: {}", taskType);
            return false;
        }

        // Currently only support export type tasks
        if (!"export".equals(taskType)) {
            logger.error("Unsupported task type: {}", taskType);
            return false;
        }

        return true;
    }
}
```

**Key Points:**
- `processExportTask()`: Simulates the 10-second task from the original synchronous endpoint
- `validateTaskParameters()`: Validates message before processing
- Comprehensive logging for CloudWatch monitoring
- Proper error handling with exceptions

---

## ExportHandler.java (Step 3)

**Location:** `src/main/java/ie/ul/csis/lambda/ExportHandler.java`

**Purpose:** Main Lambda handler that processes SQS events

```java
package ie.ul.csis.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import ie.ul.csis.lambda.model.ExportJobMessage;
import ie.ul.csis.lambda.service.ExportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportHandler implements RequestHandler<SQSEvent, String> {

    private static final Logger logger = LoggerFactory.getLogger(ExportHandler.class);
    private final ExportService exportService;
    private final ObjectMapper objectMapper;

    // Constructor for Lambda runtime
    public ExportHandler() {
        this.exportService = new ExportService();
        this.objectMapper = new ObjectMapper();
    }

    // Constructor for testing
    public ExportHandler(ExportService exportService) {
        this.exportService = exportService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String handleRequest(SQSEvent event, Context context) {
        logger.info("Lambda function started. Request ID: {}", context.getAwsRequestId());
        logger.info("Received SQS event with {} messages", event.getRecords().size());

        int successCount = 0;
        int failureCount = 0;

        // Process each SQS message
        for (SQSEvent.SQSMessage message : event.getRecords()) {
            try {
                logger.info("Processing message ID: {}", message.getMessageId());
                processMessage(message);
                successCount++;
                logger.info("Message processed successfully: {}", message.getMessageId());

            } catch (Exception e) {
                failureCount++;
                logger.error("Failed to process message ID: {}, Error: {}",
                        message.getMessageId(), e.getMessage(), e);

                // In production, you might want to send failed messages to a DLQ
                // For this lab, we'll just log the error and continue
            }
        }

        String result = String.format("Processing completed. Success: %d, Failures: %d",
                successCount, failureCount);
        logger.info(result);
        return result;
    }

    private void processMessage(SQSEvent.SQSMessage message) throws Exception {
        String messageBody = message.getBody();
        logger.info("Message body: {}", messageBody);

        // Parse the JSON message into our model object
        ExportJobMessage jobMessage;
        try {
            jobMessage = objectMapper.readValue(messageBody, ExportJobMessage.class);
            logger.info("Parsed job message: {}", jobMessage);
        } catch (Exception e) {
            logger.error("Failed to parse message body as JSON: {}", messageBody);
            throw new RuntimeException("Invalid message format", e);
        }

        // Validate the message content
        if (!exportService.validateTaskParameters(jobMessage.getJobId(), jobMessage.getTaskType())) {
            throw new RuntimeException("Invalid task parameters in message");
        }

        // Process the export task
        String result = exportService.processExportTask(
                jobMessage.getJobId(),
                jobMessage.getTaskType()
        );

        logger.info("Task processing result: {}", result);
    }
}
```

**Key Points:**
- Implements `RequestHandler<SQSEvent, String>` for SQS event handling
- `handleRequest()`: Main entry point invoked by Lambda runtime
- Processes messages in batch (SQS can send multiple messages)
- Individual error handling for each message
- All operations logged to CloudWatch

**Lambda Configuration:**
- **Handler:** `ie.ul.csis.lambda.ExportHandler::handleRequest`
- **Runtime:** Java 21
- **Memory:** 512 MB
- **Timeout:** 30 seconds

---

## pom.xml

**Location:** `pom.xml` (project root)

**Purpose:** Maven project configuration with all dependencies

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>export-lambda-function</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.amazonaws</groupId>
            <artifactId>aws-lambda-java-core</artifactId>
            <version>1.4.0</version>
        </dependency>

        <dependency>
            <groupId>com.amazonaws</groupId>
            <artifactId>aws-lambda-java-events</artifactId>
            <version>3.16.0</version>
        </dependency>

        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>dynamodb</artifactId>
            <version>2.34.1</version>
        </dependency>

        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.20.0</version>
        </dependency>

        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
            <version>2.0.17</version>
        </dependency>

        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.13.4</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                </configuration>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.4.1</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <createDependencyReducedPom>false</createDependencyReducedPom>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>ie.ul.csis.lambda.ExportHandler</mainClass>
                                </transformer>
                            </transformers>
                        </configuration>
                    </execution>
                </executions>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.1.2</version>
            </plugin>
        </plugins>
    </build>
</project>
```

**Key Dependencies:**
- **aws-lambda-java-core**: Required for Lambda function interface
- **aws-lambda-java-events**: Provides SQSEvent class
- **jackson-databind**: JSON parsing library
- **dynamodb**: AWS SDK for DynamoDB (used in Step 4)
- **slf4j-simple**: Logging implementation

**Maven Shade Plugin:**
- Creates a "fat JAR" (uber JAR) with all dependencies
- Required for Lambda deployment
- Output: `target/export-lambda-function-1.0.0.jar`

---

## Building and Deployment

### 1. Build the JAR

```bash
mvn clean package
```

**Expected output:**
```
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

**Output file:** `target/export-lambda-function-1.0.0.jar` (~15-20 MB)

### 2. Deploy to AWS Lambda

1. Create Lambda function in AWS Console
2. Upload JAR file
3. Configure handler: `ie.ul.csis.lambda.ExportHandler::handleRequest`
4. Set timeout: 30 seconds
5. Set memory: 512 MB

### 3. Add SQS Trigger

1. Add SQS as trigger
2. Select queue from Step 2
3. Set batch size: 5

---

## Testing

### Test Message Format

Send this JSON to SQS queue:

```json
{
  "jobId": "test-12345",
  "taskType": "export",
  "timestamp": "2025-09-30T10:00:00Z"
}
```

### Expected CloudWatch Logs (Step 3)

```
START RequestId: abc-123-def
[INFO] Lambda function started. Request ID: abc-123-def
[INFO] Received SQS event with 1 messages
[INFO] Processing message ID: xxx-yyy-zzz
[INFO] Message body: {"jobId":"test-12345","taskType":"export","timestamp":"2025-09-30T10:00:00Z"}
[INFO] Parsed job message: ExportJobMessage{jobId='test-12345', taskType='export', timestamp='2025-09-30T10:00:00Z'}
[INFO] Starting export task processing - JobId: test-12345, TaskType: export
[INFO] Executing export task... JobId: test-12345
[INFO] Export task completed - JobId: test-12345
[INFO] Task processing result: Export completed successfully for job: test-12345
[INFO] Message processed successfully: xxx-yyy-zzz
[INFO] Processing completed. Success: 1, Failures: 0
END RequestId: abc-123-def
REPORT RequestId: abc-123-def Duration: 10XXX.XX ms Billed Duration: 10XXX ms
```

---

## Common Issues

### Issue 1: Class Not Found
```
java.lang.ClassNotFoundException: ie.ul.csis.lambda.ExportHandler
```
**Solution:** Verify handler is `ie.ul.csis.lambda.ExportHandler::handleRequest`

### Issue 2: JSON Parsing Error
```
Failed to parse message body as JSON
```
**Solution:** Check message format matches `ExportJobMessage` structure

### Issue 3: Lambda Timeout
```
Task timed out after 3.00 seconds
```
**Solution:** Increase timeout to 30 seconds in Lambda configuration

---

# Step 4: DynamoDB Integration

This section describes the modifications needed to integrate DynamoDB status tracking into the Lambda function from Step 3.

## Additional Imports

Add these imports to the top of `ExportHandler.java`:

```java
// Step 4: Added DynamoDB imports
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.HashMap;
import java.util.Map;
```

---

## Additional Fields and Constants

Add these fields and constants to the `ExportHandler` class:

```java
public class ExportHandler implements RequestHandler<SQSEvent, String> {

    private static final Logger logger = LoggerFactory.getLogger(ExportHandler.class);
    private final ExportService exportService;
    private final ObjectMapper objectMapper;
    
    // Step 4: Added DynamoDB client and table configuration
    private final DynamoDbClient dynamoDbClient;
    private static final String TABLE_NAME = "JobTable";
    private static final String PRIMARY_KEY = "jobId";
    
    // ... rest of class
}
```

**Key Points:**
- `dynamoDbClient`: Client for connecting to DynamoDB
- `TABLE_NAME`: Name of the DynamoDB table (must match Step 2)
- `PRIMARY_KEY`: Primary key field name in DynamoDB table

---

## Modified Constructor

Replace the existing `ExportHandler()` constructor with:

```java
// Constructor for Lambda runtime
public ExportHandler() {
    this.exportService = new ExportService();
    this.objectMapper = new ObjectMapper();
    
    // Step 4: Initialize DynamoDB client
    this.dynamoDbClient = DynamoDbClient.builder()
            .region(Region.EU_NORTH_1)
            .build();
}
```

**Key Points:**
- Initializes DynamoDB client with EU_NORTH_1 region
- Client is created once per Lambda container (reused across invocations)
- No credentials needed - Lambda execution role provides authentication

**Also update the testing constructor:**

```java
// Constructor for testing
public ExportHandler(ExportService exportService) {
    this.exportService = exportService;
    this.objectMapper = new ObjectMapper();
    
    // Step 4: Initialize DynamoDB client for testing
    this.dynamoDbClient = DynamoDbClient.builder()
            .region(Region.EU_NORTH_1)
            .build();
}
```

---

## Modified processMessage Method

Replace the existing `processMessage()` method with:

```java
private void processMessage(SQSEvent.SQSMessage message) throws Exception {
    String messageBody = message.getBody();
    logger.info("Message body: {}", messageBody);

    // Parse the JSON message into our model object
    ExportJobMessage jobMessage;
    try {
        jobMessage = objectMapper.readValue(messageBody, ExportJobMessage.class);
        logger.info("Parsed job message: {}", jobMessage);
    } catch (Exception e) {
        logger.error("Failed to parse message body as JSON: {}", messageBody);
        throw new RuntimeException("Invalid message format", e);
    }

    // Validate the message content
    if (!exportService.validateTaskParameters(jobMessage.getJobId(), jobMessage.getTaskType())) {
        throw new RuntimeException("Invalid task parameters in message");
    }

    // Process the export task
    String result = exportService.processExportTask(
            jobMessage.getJobId(),
            jobMessage.getTaskType()
    );
    logger.info("Task result: {}", result);

    // Step 4: Update job status to Completed in DynamoDB
    updateJobStatus(jobMessage.getJobId(), "Completed");
}
```

**What changed:**
- Added call to `updateJobStatus()` at the end
- This updates DynamoDB status to "Completed" after successful task execution

---

## New updateJobStatus Method

Add this new method to the `ExportHandler` class:

```java
// Step 4: Method to update job status in DynamoDB
private void updateJobStatus(String jobId, String status) {
    logger.info("Updating job {} status to {}", jobId, status);
    
    Map<String, AttributeValue> key = new HashMap<>();
    key.put(PRIMARY_KEY, AttributeValue.builder().s(jobId).build());

    Map<String, AttributeValue> expressionValues = new HashMap<>();
    expressionValues.put(":status", AttributeValue.builder().s(status).build());

    UpdateItemRequest updateRequest = UpdateItemRequest.builder()
            .tableName(TABLE_NAME)
            .key(key)
            .updateExpression("SET #status = :status")
            .expressionAttributeNames(Map.of("#status", "status"))
            .expressionAttributeValues(expressionValues)
            .build();

    dynamoDbClient.updateItem(updateRequest);
    logger.info("Job {} status updated successfully.", jobId);
}
```

**How it works:**
1. Creates a key map with the jobId (primary key)
2. Creates expression values map with the new status
3. Builds an UpdateItemRequest with:
   - Table name
   - Primary key to identify the item
   - Update expression to set status field
   - Expression attribute names (to avoid reserved word conflicts)
   - Expression attribute values
4. Executes the update operation
5. Logs success

**Key Points:**
- Uses `#status` as placeholder for field name (DynamoDB reserved word handling)
- Uses `:status` as placeholder for the value
- Updates only the `status` field, leaves other fields unchanged

---

## Rebuild and Redeploy

After making Step 4 changes:

```bash
mvn clean package
```

Upload the new JAR to Lambda, then configure IAM permissions before testing.
