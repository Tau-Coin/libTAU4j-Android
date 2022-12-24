package io.taucoin.news.publishing.core.model.data.message;

public enum QueueOperation {
    INSERT(0),
    UPDATE(1),
    DELETE(2),
    ON_CHAIN(3),
    ROLL_BACK(4);

    private int operation;
    QueueOperation(int operation) {
        this.operation = operation;
    }

    public int getOperation() {
        return operation;
    }
}
