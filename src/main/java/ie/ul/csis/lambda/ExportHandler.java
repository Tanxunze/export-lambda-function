package ie.ul.csis.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import ie.ul.csis.lambda.model.ExportJobMessage;
import ie.ul.csis.lambda.service.ExportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// AWS Lambda function handler for processing export tasks from SQS messages

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