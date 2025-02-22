package com.sisal.transaction.server.util;

import com.sisal.transaction.server.model.api.TransactionAPIRequest;
import com.sisal.transaction.server.model.api.TransactionAPIResponse;
import com.sisal.transaction.server.model.rest.TransactionRequest;
import com.sisal.transaction.server.model.rest.TransactionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    // Request mappings
    @Mapping(source = "accountNumber", target = "accountNumber")
    @Mapping(source = "transactionType", target = "transactionType")
    @Mapping(source = "amount", target = "amount")
    TransactionAPIRequest fromRestToAPIRequest(TransactionRequest restRequest);

    @Mapping(source = "accountNumber", target = "accountNumber")
    @Mapping(source = "transactionType", target = "transactionType")
    @Mapping(source = "amount", target = "amount")
    TransactionRequest fromAPIToRestRequest(TransactionAPIRequest apiRequest);

    // Response mappings
    @Mapping(source = "transactionId", target = "transactionId")
    @Mapping(source = "accountNumber", target = "accountNumber")
    @Mapping(source = "transactionType", target = "transactionType")
    @Mapping(source = "amount", target = "amount")
    @Mapping(source = "timestamp", target = "timestamp")
    @Mapping(source = "status", target = "status")
    TransactionResponse fromAPIToRestResponse(TransactionAPIResponse apiResponse);

    @Mapping(source = "transactionId", target = "transactionId")
    @Mapping(source = "accountNumber", target = "accountNumber")
    @Mapping(source = "transactionType", target = "transactionType")
    @Mapping(source = "amount", target = "amount")
    @Mapping(source = "timestamp", target = "timestamp")
    @Mapping(source = "status", target = "status")
    TransactionAPIResponse fromRestToAPIResponse(TransactionResponse restResponse);

}
