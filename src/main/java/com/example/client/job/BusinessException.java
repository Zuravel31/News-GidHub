package com.example.client.job;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final String errorCode; // Добавляем код ошибки

    public BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}