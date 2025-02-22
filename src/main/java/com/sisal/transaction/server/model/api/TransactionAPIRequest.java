package com.sisal.transaction.server.model.api;

import java.util.Objects;

/**
 * TransactionRequest
 */
public class TransactionAPIRequest {

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

    public TransactionAPIRequest accountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
        return this;
    }

    /**
     * Source account identifier.
     *
     * @return accountNumber
     **/
    public String getaccountNumber() {
        return accountNumber;
    }

    public void setaccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public TransactionAPIRequest transactionType(TransactionTypeEnum transactionType) {
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

    public TransactionAPIRequest amount(Double amount) {
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


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransactionAPIRequest transactionAPIRequest = (TransactionAPIRequest) o;
        return Objects.equals(this.accountNumber, transactionAPIRequest.accountNumber) &&
                Objects.equals(this.transactionType, transactionAPIRequest.transactionType) &&
                Objects.equals(this.amount, transactionAPIRequest.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountNumber, transactionType, amount);
    }

    @Override
    public String toString() {

        return "TransactionRequest {\n" +
                "    accountNumber: " + toIndentedString(accountNumber) + "\n" +
                "    transactionType: " + toIndentedString(transactionType) + "\n" +
                "    amount: " + toIndentedString(amount) + "\n" +
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

