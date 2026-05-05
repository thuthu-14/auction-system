package common;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private MessageType type;
    private Object data;
    private String senderId;
    private long timestamp;
    private String status;
    private String message;

    public Message(MessageType type, Object data, String senderId) {
        this.type = type;
        this.data = data;
        this.senderId = senderId;
        this.timestamp = System.currentTimeMillis();
        this.status = "SUCCESS";
    }

    public Message(MessageType type, String status, String message) {
        this.type = type;
        this.status = status;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public Message() {}

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Message{" + "type=" + type + ", status='" + status + '\'' + '}';
    }
}


