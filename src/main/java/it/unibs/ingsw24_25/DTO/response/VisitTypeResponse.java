package it.unibs.ingsw24_25.DTO.response;

import it.unibs.ingsw24_25.model.VisitStatus;
import java.time.LocalDate;

public record VisitTypeResponse(
        String id,
        String title,
        String description,
        String meetLocation,
        LocalDate validFrom,
        LocalDate validTo,
        boolean ticketRequired,
        int minParticipants,
        int maxParticipants,
        VisitStatus status
) {}
