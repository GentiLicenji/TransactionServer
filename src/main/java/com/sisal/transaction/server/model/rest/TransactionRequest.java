package com.sisal.transaction.server.model.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * TransactionRequest
 */
@Validated
@javax.annotation.Generated(value = "com.glic.GentiSpringCodegen", date = "2025-02-20T13:46:34.637+01:00")

public class TransactionRequest {
    @JsonProperty("accountNumber")
    private String accountNumber = null;

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

    public TransactionRequest accountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
        return this;
    }

    /**
     * Source account identifier.
     *
     * @return accountNumber
     **/
    @ApiModelProperty(required = true, value = "Source account identifier.")
    @NotNull


    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public TransactionRequest transactionType(TransactionTypeEnum transactionType) {
        this.transactionType = transactionType;
        return this;
    }

    /**
     * The type of transaction.
     *
     * @return transactionType
     **/
    @ApiModelProperty(required = true, value = "The type of transaction.")
    @NotNull


    public TransactionTypeEnum getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionTypeEnum transactionType) {
        this.transactionType = transactionType;
    }

    public TransactionRequest amount(Double amount) {
        this.amount = amount;
        return this;
    }

    /**
     * The amount of the transaction.
     * maximum: 10000
     *
     * @return amount
     **/
    @ApiModelProperty(required = true, value = "The amount of the transaction.")
    @NotNull

    @DecimalMax("10000")
    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransactionRequest transactionRequest = (TransactionRequest) o;
        return Objects.equals(this.accountNumber, transactionRequest.accountNumber) &&
                Objects.equals(this.transactionType, transactionRequest.transactionType) &&
                Objects.equals(this.amount, transactionRequest.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountNumber, transactionType, amount);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class TransactionRequest {\n");

        sb.append("    accountNumber: ").append(toIndentedString(accountNumber)).append("\n");
        sb.append("    transactionType: ").append(toIndentedString(transactionType)).append("\n");
        sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
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

