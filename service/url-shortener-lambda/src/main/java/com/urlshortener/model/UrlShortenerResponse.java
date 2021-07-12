package com.urlshortener.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UrlShortenerResponse {
    private String shortUrl;
    private Boolean hasError;
    private ServiceError serviceError;
}
