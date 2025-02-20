package com.sisal.transaction.server.service;


import com.sisal.transaction.server.model.api.TransactionAPIRequest;
import com.sisal.transaction.server.model.api.TransactionAPIResponse;
import org.springframework.stereotype.Service;

@Service
public class TransactionApiService {

    public TransactionAPIResponse createTransaction(TransactionAPIRequest apiRequest) {
        return new TransactionAPIResponse();
    }

}