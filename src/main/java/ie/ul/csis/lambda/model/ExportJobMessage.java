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