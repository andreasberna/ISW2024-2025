package it.unibs.ingsw24_25.util;

import it.unibs.ingsw24_25.DTO.*;
import it.unibs.ingsw24_25.DTO.response.PlaceResponse;
import it.unibs.ingsw24_25.DTO.response.VolunteerResponse;
import it.unibs.ingsw24_25.model.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class DTOMapper {

    private DTOMapper() {
    }

    public static PlaceResponse placeToDTO(Place p){
        return new PlaceResponse (p.getId(), p.getPlaceTitle (), p.getPlaceDescription (), p.getLocation ());
    }

    public static VolunteerResponse volunteerToDTO(Volunteer v){
        return new VolunteerResponse(v.getNickname(), v.isActive(), v.isFirstAccessPending());
    }

    public static AssignedShiftDTO toAssignedShiftDTO(AssignedShift shift) {
        return new AssignedShiftDTO(
                shift.getMonth(),
                shift.getDate(),
                shift.getVisitTypeId(),
                shift.getSlot()
        );
    }

    public static MonthlyPlanDTO planToDTO(MonthlyVisitPlan plan, List<VisitType> visitTypes) {
        if (plan == null) {
            return null;
        }
        Map<String, VisitType> visitTypeMap = visitTypes == null
                ? Map.of()
                : visitTypes.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(VisitType::getId, visitType -> visitType, (left, right) -> left));

        List<PlannedVisitDTO> plannedVisits = plan.getVisits().stream()
                .filter(Objects::nonNull)
                .map(DTOMapper::toPlannedVisitDTO)
                .collect(Collectors.toList());

        return new MonthlyPlanDTO(
                plan.getTargetMonth(),
                plan.getPhase(),
                null,
                plannedVisits,
                null
        );
    }

    public static VisitOccurrenceDTO toVisitOccurrenceDTO(MonthlyVisitPlan plan,
                                                          PlannedVisit plannedVisit,
                                                          VisitType visitType,
                                                          boolean includeBookings) {
        if (plannedVisit == null) {
            return null;
        }

        int bookedParticipants = plannedVisit.countBookedParticipants();

        List<VisitBookingDTO> bookingDTOs = includeBookings
                ? plannedVisit.getBookings().stream()
                        .map(b -> new VisitBookingDTO(
                                b.getCode(),
                                b.getBeneficiaryName(),
                                b.getParticipants(),
                                b.getNotes(),
                                plannedVisit.getVisitDate(),
                                visitType != null ? visitType.getVisitTitle() : "",
                                plannedVisit.getStatus()))
                        .collect(Collectors.toList())
                : List.of();

        return new VisitOccurrenceDTO(
                plannedVisit.getId().toString(),
                plan.getTargetMonth(),
                plannedVisit.getVisitDate(),
                plannedVisit.getVisitTime(),
                visitType != null ? visitType.getVisitTitle() : plannedVisit.getVisitType().getId(),
                visitType != null ? visitType.getDescription() : "",
                visitType != null ? visitType.getMeetLocation() : "",
                visitType != null && visitType.isTicketRequired(),
                visitType != null ? visitType.getMinParticipants() : 0,
                visitType != null ? visitType.getMaxParticipants() : 0,
                bookedParticipants,
                plannedVisit.getStatus(),
                plannedVisit.getVisitType().getId(),
                bookingDTOs,
                plan.getId(),
                visitType != null ? visitType.getVisitTitle() : null,
                plannedVisit.getVolunteer() != null ? plannedVisit.getVolunteer().getNickname() : null
        );
    }

    private static PlannedVisitDTO toPlannedVisitDTO(PlannedVisit visit) {
        return new PlannedVisitDTO(
                visit.getId(),
                visit.getVisitType().getVisitTitle(),
                visit.getVolunteer() != null ? visit.getVolunteer().getNickname() : null,
                visit.getVisitDate(),
                visit.getVisitTime(),
                visit.getStatus().toString()
        );
    }
}
