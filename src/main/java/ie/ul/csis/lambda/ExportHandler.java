package ie.ul.csis.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import ie.ul.csis.lambda.model.ExportJobMessage;
import ie.ul.csis.lambda.service.ExportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import java.util.HashMap;
import java.util.Map;

public class ExportHandler implements RequestHandler<SQSEvent, String> {

    private static final Logger logger = LoggerFactory.getLogger(ExportHandler.class);
    private final ExportService exportService;
    private final ObjectMapper objectMapper;

    private final DynamoDbClient dynamoDbClient;
    private static final String TABLE_NAME = System.getenv("DYNAMODB_TABLE_NAME");
    private static final String PRIMARY_KEY = System.getenv().getOrDefault("PRIMARY_KEY", "jobId");
    private static final String AWS_REGION = System.getenv().getOrDefault("AWS_REGION", "eu-north-1");

    public ExportHandler() {
        this.exportService = new ExportService();
        this.objectMapper = new ObjectMapper();
        this.dynamoDbClient = DynamoDbClient.builder()
                .region(Region.of(AWS_REGION))
                .build();
    }

    // Constructor for testing
    public ExportHandler(ExportService exportService) {
        this.exportService = exportService;
        this.objectMapper = new ObjectMapper();
        this.dynamoDbClient = DynamoDbClient.builder()
                .region(Region.of(AWS_REGION))
                .build();
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
        logger.info("Task result: {}", result);

        updateJobStatus(jobMessage.getJobId(), "Completed");
    }

    // update job status in DynamoDB
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
}
