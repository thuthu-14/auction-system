package server.exception;

public class ServerException extends RuntimeException {
    private final ServerErrorType type;
    private final String clientMessage;

    public ServerException(ServerErrorType type, String clientMessage) {
        super(clientMessage);
        this.type = type != null ? type : ServerErrorType.SYSTEM;
        this.clientMessage = clientMessage;
    }

    public ServerException(ServerErrorType type, String clientMessage, Throwable cause) {
        super(clientMessage, cause);
        this.type = type != null ? type : ServerErrorType.SYSTEM;
        this.clientMessage = clientMessage;
    }

    public ServerErrorType getType() {
        return type;
    }

    public String getClientMessage() {
        return clientMessage;
    }
}
