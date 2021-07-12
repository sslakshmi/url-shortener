package com.urlshortener.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServiceError {
    private String code;
    private String message;
}
