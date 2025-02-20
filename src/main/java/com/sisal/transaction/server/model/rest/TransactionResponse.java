package com.sisal.transaction.server.model.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * TransactionResponse
 */
@Validated
@javax.annotation.Generated(value = "com.glic.GentiSpringCodegen", date = "2025-02-20T13:46:34.637+01:00")


public class TransactionResponse {
    @JsonProperty("transactionId")
    private UUID transactionId = null;

    @JsonProperty("accountId")
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
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static TransactionTypeEnum fromValue(String text) {
            for (TransactionTypeEnum b : TransactionTypeEnum.values()) {
                if (String.valueOf(b.value).equals(text)) {
                    return b;
                }
            }
            return null;
        }
    }

    @JsonProperty("transactionType")
    private TransactionTypeEnum transactionType = null;

    @JsonProperty("amount")
    private Double amount = null;

    @JsonProperty("timestamp")
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
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static StatusEnum fromValue(String text) {
            for (StatusEnum b : StatusEnum.values()) {
                if (String.valueOf(b.value).equals(text)) {
                    return b;
                }
            }
            return null;
        }
    }

    @JsonProperty("status")
    private StatusEnum status = null;

    public TransactionResponse transactionId(UUID transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    /**
     * Unique identifier for the transaction.
     *
     * @return transactionId
     **/
    @ApiModelProperty(required = true, value = "Unique identifier for the transaction.")
    @NotNull

    @Valid

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public TransactionResponse accountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    /**
     * Source account identifier.
     *
     * @return accountId
     **/
    @ApiModelProperty(required = true, value = "Source account identifier.")
    @NotNull


    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public TransactionResponse transactionType(TransactionTypeEnum transactionType) {
        this.transactionType = transactionType;
        return this;
    }

    /**
     * The type of transaction.
     *
     * @return transactionType
     **/
    @ApiModelProperty(value = "The type of transaction.")


    public TransactionTypeEnum getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionTypeEnum transactionType) {
        this.transactionType = transactionType;
    }

    public TransactionResponse amount(Double amount) {
        this.amount = amount;
        return this;
    }

    /**
     * The amount of the transaction.
     * maximum: 10000
     *
     * @return amount
     **/
    @ApiModelProperty(value = "The amount of the transaction.")

    @DecimalMax("10000")
    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public TransactionResponse timestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    /**
     * Date and time of the transaction.
     *
     * @return timestamp
     **/
    @ApiModelProperty(value = "Date and time of the transaction.")

    @Valid

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public TransactionResponse status(StatusEnum status) {
        this.status = status;
        return this;
    }

    /**
     * Current status of the transaction.
     *
     * @return status
     **/
    @ApiModelProperty(required = true, value = "Current status of the transaction.")
    @NotNull


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
        TransactionResponse transactionResponse = (TransactionResponse) o;
        return Objects.equals(this.transactionId, transactionResponse.transactionId) &&
                Objects.equals(this.accountId, transactionResponse.accountId) &&
                Objects.equals(this.transactionType, transactionResponse.transactionType) &&
                Objects.equals(this.amount, transactionResponse.amount) &&
                Objects.equals(this.timestamp, transactionResponse.timestamp) &&
                Objects.equals(this.status, transactionResponse.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, accountId, transactionType, amount, timestamp, status);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class TransactionResponse {\n");

        sb.append("    transactionId: ").append(toIndentedString(transactionId)).append("\n");
        sb.append("    accountId: ").append(toIndentedString(accountId)).append("\n");
        sb.append("    transactionType: ").append(toIndentedString(transactionType)).append("\n");
        sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
        sb.append("    timestamp: ").append(toIndentedString(timestamp)).append("\n");
        sb.append("    status: ").append(toIndentedString(status)).append("\n");
        sb.append("}");
        return sb.toString();
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

