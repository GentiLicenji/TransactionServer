package com.sisal.transaction.server.delegate;

import com.sisal.transaction.server.model.api.TransactionAPIRequest;
import com.sisal.transaction.server.model.api.TransactionAPIResponse;
import com.sisal.transaction.server.model.rest.TransactionRequest;
import com.sisal.transaction.server.model.rest.TransactionResponse;
import com.sisal.transaction.server.service.TransactionApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class TransactionApiDelegate {
    private final TransactionApiService transactionApiService;

    @Autowired
    public TransactionApiDelegate(TransactionApiService transactionApiService) {
        this.transactionApiService = transactionApiService;
    }

    public ResponseEntity<TransactionResponse> createTransaction(TransactionRequest restRequest) {

        TransactionAPIResponse apiResponse = transactionApiService.createTransaction(new TransactionAPIRequest());

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

}
