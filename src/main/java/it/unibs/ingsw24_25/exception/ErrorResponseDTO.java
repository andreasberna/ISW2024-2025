package it.unibs.ingsw24_25.exception;

import java.time.LocalDateTime;

public record ErrorResponseDTO(
    LocalDateTime timestamp,
    int status,
    String error,
    String message
) {}
