package client.exception;

public class ClientException extends RuntimeException {
    private final ClientErrorType type;
    private final String userMessage;

    public ClientException(ClientErrorType type, String userMessage) {
        super(userMessage);
        this.type = type != null ? type : ClientErrorType.UNKNOWN;
        this.userMessage = userMessage;
    }

    public ClientException(ClientErrorType type, String userMessage, Throwable cause) {
        super(userMessage, cause);
        this.type = type != null ? type : ClientErrorType.UNKNOWN;
        this.userMessage = userMessage;
    }

    public ClientErrorType getType() {
        return type;
    }

    public String getUserMessage() {
        return userMessage;
    }
}
