package it.unibs.ingsw24_25.util;

import it.unibs.ingsw24_25.DTO.*;
import it.unibs.ingsw24_25.model.*;

import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class DTOMapper {

    private DTOMapper() {
    }

    public static PlaceDTO placeToDTO(Place p){
        return new PlaceDTO (p.getPlaceTitle (), p.getPlaceDescription (), p.getLocation ());
    }

    public static VisitTypeDTO visitTypeToDTO(VisitType vt){
        String daySummary = summarizeDays(vt.getSchedules ());
        String start = startTimeString (vt);
        String end = endTimeString (vt);
        int duration = durationMinutes (vt);
        return new VisitTypeDTO (
                vt.getId (), vt.getVisitTitle (), daySummary, start, end,
                duration, vt.getTicketRequired (),
                vt.getMinParticipants (), vt.getMaxParticipants (), vt.getPlace ().getPlaceTitle (), vt.getState ());

    }

    public static VolunteerDTO volunteerToDTO(Volunteer v){
        List<String> titles = v.getVisitsAttending ().stream().
                map(VisitType::getVisitTitle)
                .toList ();

        List<VolunteerAvailabilityDTO> availabilityDTOs = v.getAvailabilities ().entrySet ().stream ()
                .sorted (java.util.Map.Entry.comparingByKey ())
                .map (entry -> new VolunteerAvailabilityDTO(
                        entry.getKey (),
                        entry.getValue ().getPreferredDays ().stream ().toList (),
                        entry.getValue ().getWeeklyFrequency (),
                        entry.getValue ().getSubmittedOn ()
                ))
                .toList ();

        List<AssignedShiftDTO> shiftsDTOs = v.getScheduledShifts ().entrySet ().stream ()
                .flatMap (entry -> entry.getValue ().stream ()
                        .map(shift -> new AssignedShiftDTO(
                                entry.getKey (),
                                shift.getDate (),
                                shift.getVisitTypeId (),
                                shift.getSlot ()
                        )))
                .sorted((left, right) -> compareShift(left, right))
                .toList ();

        return new VolunteerDTO (v.getNickname (), v.getPassword (), v.isFirstAccessPending (), titles, availabilityDTOs, shiftsDTOs);
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

        List<PlannedVisitDTO> plannedVisits = plan.getPlannedVisits().stream()
                .filter(Objects::nonNull)
                .map(visit -> toPlannedVisitDTO(plan, visit, visitTypeMap.get(visit.getVisitTypeId())))
                .sorted(DTOMapper::comparePlannedVisits)
                .toList();

        List<PlanAvailabilityDTO> availabilitySnapshots = plan.getAvailabilitySnapshots().entrySet().stream()
                .map(entry -> toPlanAvailabilityDTO(entry.getKey(), entry.getValue()))
                .filter(Objects::nonNull)
                .sorted((left, right) -> left.getVolunteerNickname().compareToIgnoreCase(right.getVolunteerNickname()))
                .toList();

        return new MonthlyPlanDTO(
                plan.getTargetMonth(),
                plan.getPhase(),
                plan.getAvailabilityWindowClosedOn(),
                plannedVisits,
                availabilitySnapshots
        );
    }

    private static PlannedVisitDTO toPlannedVisitDTO(MonthlyVisitPlan plan,
                                                     PlannedVisit visit,
                                                     VisitType visitType) {
        TimeSlot slot = visit.getTimeSlot();
        return new PlannedVisitDTO(
                plan.getTargetMonth(),
                visit.getDate(),
                slot != null && slot.getDay() != null ? slot.getDay() : visit.getDate().getDayOfWeek(),
                slot != null ? slot.getStartTime() : null,
                slot != null && slot.getDuration() != null ? slot.getDuration().toMinutes() : 0,
                visit.getVisitTypeId(),
                visitType != null && visitType.getVisitTitle() != null ? visitType.getVisitTitle() : visit.getVisitTypeId(),
                visit.isProposable(),
                visit.getAssignedVolunteerIds()
        );
    }

    private static int comparePlannedVisits(PlannedVisitDTO left, PlannedVisitDTO right) {
        int dateComparison = left.getDate().compareTo(right.getDate());
        if (dateComparison != 0) {
            return dateComparison;
        }
        if (left.getStartTime() != null && right.getStartTime() != null) {
            int timeComparison = left.getStartTime().compareTo(right.getStartTime());
            if (timeComparison != 0) {
                return timeComparison;
            }
        }
        return left.getVisitTypeId().compareToIgnoreCase(right.getVisitTypeId());
    }

    private static PlanAvailabilityDTO toPlanAvailabilityDTO(String nickname, MonthlyAvailability availability) {
        if (availability == null) {
            return null;
        }
        return new PlanAvailabilityDTO(
                nickname,
                availability.getReferenceMonth(),
                availability.getPreferredDays().stream().sorted().toList(),
                availability.getWeeklyFrequency(),
                availability.getSubmittedOn(),
                availability.getSnapshotCapturedOn()
        );
    }

    private static int compareShift(AssignedShiftDTO left, AssignedShiftDTO right) {
        int monthComparison = left.getMonth().compareTo(right.getMonth());
        if (monthComparison != 0) {
            return monthComparison;
        }
        int dateComparison = left.getDate().compareTo(right.getDate());
        if (dateComparison != 0) {
            return dateComparison;
        }
        return left.getVisitTypeId().compareTo(right.getVisitTypeId());
    }


    private static String summarizeDays(List<TimeSlot> slots){
        var days = slots.stream()
                .map(TimeSlot::getDay)
                .distinct()
                .sorted()
                .map(d -> d.getDisplayName (TextStyle.SHORT, Locale.ITALIAN))
                .map(s -> s.substring (0,1).toUpperCase (Locale.ITALIAN) + s.substring (1) )
                .collect(Collectors.joining(", "));
        return days;
    }

    private static String startTimeString(VisitType vt){
        if(vt.getSchedules ().isEmpty()) return "-";

        return vt.getSchedules ().get(0).getStartTime().toString();
    }

    private static String endTimeString(VisitType vt){
        if(vt.getSchedules ().isEmpty()) return "-";

        var slot = vt.getSchedules ().get(0);
        return slot.getStartTime ().plus (slot.getDuration ()).toString ();
    }

    private static int durationMinutes(VisitType vt){
        if(vt.getSchedules ().isEmpty()) return 0;
        var slot = vt.getSchedules ().get(0);
        return  (int) slot.getDuration ().toMinutes();

    }
}