package io.taucoin.torrent.publishing.core.model.data.message;

public enum QueueOperation {
    INSERT(0),
    UPDATE(1),
    DELETE(2);

    private int operation;
    QueueOperation(int operation) {
        this.operation = operation;
    }

    public int getOperation() {
        return operation;
    }
}
