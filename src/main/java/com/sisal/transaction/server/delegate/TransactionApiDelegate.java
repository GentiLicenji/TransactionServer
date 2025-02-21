package com.sisal.transaction.server.delegate;

import com.sisal.transaction.server.model.api.TransactionAPIRequest;
import com.sisal.transaction.server.model.api.TransactionAPIResponse;
import com.sisal.transaction.server.model.rest.TransactionRequest;
import com.sisal.transaction.server.model.rest.TransactionResponse;
import com.sisal.transaction.server.service.TransactionApiService;
import com.sisal.transaction.server.util.TransactionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class TransactionApiDelegate {

    private final TransactionApiService transactionApiService;
    private final TransactionMapper transactionMapper;

    @Autowired
    public TransactionApiDelegate(TransactionApiService transactionApiService, TransactionMapper transactionMapper) {
        // Add debug logging
        System.out.println("Mapper class: " + transactionMapper.getClass().getName());
        this.transactionApiService = transactionApiService;
        this.transactionMapper = transactionMapper;
    }

    public ResponseEntity<TransactionResponse> createTransaction(TransactionRequest restRequest) {

        TransactionAPIRequest apiRequest = transactionMapper.fromRestToAPIRequest(restRequest);

        TransactionAPIResponse apiResponse = transactionApiService.createTransaction(apiRequest);

        TransactionResponse restResponse = transactionMapper.fromAPIToRestResponse(apiResponse);

        return new ResponseEntity<>(restResponse, HttpStatus.CREATED);
    }

}