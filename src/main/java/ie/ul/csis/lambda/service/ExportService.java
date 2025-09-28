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