# Export Lambda Function

AWS Lambda function for processing export tasks from SQS queue (CS4297 Lab Week 5 - Step 3).

## Project Structure

```
src/main/java/ie/ul/csis/lambda/
├── ExportHandler.java              # Main Lambda handler
├── model/
│   └── ExportJobMessage.java      # Message format from SQS
└── service/
    └── ExportService.java          # Business logic (10s simulated task)
```

## Building

```bash
mvn clean package
```

The deployable JAR will be at: `target/export-lambda-function-1.0.0.jar`

## AWS Lambda Configuration

**Handler**: `ie.ul.csis.lambda.ExportHandler::handleRequest`  
**Runtime**: Java 21  
**Memory**: 512 MB (recommended)  
**Timeout**: 30 seconds (to allow for 10s processing + overhead)

## AWS SQS Trigger Setup

1. Create SQS queue in AWS Console
2. Add SQS as trigger for this Lambda function
3. Batch size: 1-10 (1 is completely ok for this lab)

## Message Format

Expected JSON message from Step 2 (Spring Boot):

```json
{
  "jobId": "123e4567-e89b-12d3-a456-426614174000",
  "taskType": "export",
  "timestamp": "2025-09-30T10:30:00Z"
}
```

