package ru.x5.motpsender.dao.dto;

import lombok.Data;

import java.util.Date;

@Data
public class ErrorResponse {
    private Date timestamp;
    private String status;
    private String error;
    private String message;
    private String path;
}
