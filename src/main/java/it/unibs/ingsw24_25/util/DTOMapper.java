package it.unibs.ingsw24_25.util;

import it.unibs.ingsw24_25.DTO.PlaceDTO;
import it.unibs.ingsw24_25.DTO.VisitTypeDTO;
import it.unibs.ingsw24_25.DTO.VolunteerDTO;
import it.unibs.ingsw24_25.model.Place;
import it.unibs.ingsw24_25.model.TimeSlot;
import it.unibs.ingsw24_25.model.VisitType;
import it.unibs.ingsw24_25.model.Volunteer;

import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
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
                vt.getVisitTitle (), daySummary, start, end,
                duration, vt.getTicketRequired (),
                vt.getMinParticipants (), vt.getMaxParticipants ());

    }

    public static VolunteerDTO volunteerToDTO(Volunteer v){
        List<String> titles = v.getVisitsAttending ().stream().
                map(VisitType::getVisitTitle)
                .toList ();
        return new VolunteerDTO (v.getNickname (), titles);
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