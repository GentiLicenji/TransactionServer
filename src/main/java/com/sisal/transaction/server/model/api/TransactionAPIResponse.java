package com.sisal.transaction.server.model.api;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * TransactionResponse
 */
public class TransactionAPIResponse {

    private UUID transactionId = null;

    private String accountId = null;

    /**
     * The type of transaction.
     */
    public enum TransactionTypeEnum {
        DEPOSIT("DEPOSIT"),

        WITHDRAWAL("WITHDRAWAL");

        private String value;

        TransactionTypeEnum(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        public static TransactionTypeEnum fromValue(String text) {
            for (TransactionTypeEnum b : TransactionTypeEnum.values()) {
                if (String.valueOf(b.value).equals(text)) {
                    return b;
                }
            }
            return null;
        }
    }

    private TransactionTypeEnum transactionType = null;

    private Double amount = null;

    private OffsetDateTime timestamp = null;

    /**
     * Current status of the transaction.
     */
    public enum StatusEnum {
        COMPLETED("COMPLETED"),

        FAILED("FAILED");

        private String value;

        StatusEnum(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        public static StatusEnum fromValue(String text) {
            for (StatusEnum b : StatusEnum.values()) {
                if (String.valueOf(b.value).equals(text)) {
                    return b;
                }
            }
            return null;
        }
    }

    private StatusEnum status = null;

    public TransactionAPIResponse transactionId(UUID transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    /**
     * Unique identifier for the transaction.
     *
     * @return transactionId
     **/
    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public TransactionAPIResponse accountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    /**
     * Source account identifier.
     *
     * @return accountId
     **/
    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public TransactionAPIResponse transactionType(TransactionTypeEnum transactionType) {
        this.transactionType = transactionType;
        return this;
    }

    /**
     * The type of transaction.
     *
     * @return transactionType
     **/
    public TransactionTypeEnum getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionTypeEnum transactionType) {
        this.transactionType = transactionType;
    }

    public TransactionAPIResponse amount(Double amount) {
        this.amount = amount;
        return this;
    }

    /**
     * The amount of the transaction.
     * maximum: 10000
     *
     * @return amount
     **/
    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public TransactionAPIResponse timestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    /**
     * Date and time of the transaction.
     *
     * @return timestamp
     **/
    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public TransactionAPIResponse status(StatusEnum status) {
        this.status = status;
        return this;
    }

    /**
     * Current status of the transaction.
     *
     * @return status
     **/
    public StatusEnum getStatus() {
        return status;
    }

    public void setStatus(StatusEnum status) {
        this.status = status;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransactionAPIResponse transactionAPIResponse = (TransactionAPIResponse) o;
        return Objects.equals(this.transactionId, transactionAPIResponse.transactionId) &&
                Objects.equals(this.accountId, transactionAPIResponse.accountId) &&
                Objects.equals(this.transactionType, transactionAPIResponse.transactionType) &&
                Objects.equals(this.amount, transactionAPIResponse.amount) &&
                Objects.equals(this.timestamp, transactionAPIResponse.timestamp) &&
                Objects.equals(this.status, transactionAPIResponse.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, accountId, transactionType, amount, timestamp, status);
    }

    @Override
    public String toString() {

        return "TransactionResponse {\n" +
                "    transactionId: " + toIndentedString(transactionId) + "\n" +
                "    accountId: " + toIndentedString(accountId) + "\n" +
                "    transactionType: " + toIndentedString(transactionType) + "\n" +
                "    amount: " + toIndentedString(amount) + "\n" +
                "    timestamp: " + toIndentedString(timestamp) + "\n" +
                "    status: " + toIndentedString(status) + "\n" +
                "}";
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}

