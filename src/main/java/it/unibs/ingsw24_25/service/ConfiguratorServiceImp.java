package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.DTO.PlaceDTO;
import it.unibs.ingsw24_25.DTO.VisitTypeDTO;
import it.unibs.ingsw24_25.DTO.VolunteerDTO;
import it.unibs.ingsw24_25.model.*;
import it.unibs.ingsw24_25.repository.*;
import it.unibs.ingsw24_25.util.DTOMapper;
import it.unibs.ingsw24_25.util.ExcludedDatePolicy;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ConfiguratorServiceImp implements ConfiguratorService {

    public static final String DEFAULT_NICKNAME = "config";
    public static final String DEFAULT_PASSWORD = "psswrd";

    private final PlaceRepository placeRepository;
    private final VisitTypeRepository visitTypeRepository;
    private final VolunteerRepository volunteerRepository;
    private final SettingsRepository settingsRepository;
    private final ConfiguratorRepository configuratorRepository;
    private boolean defaultCredentialsValidated = false;

    public ConfiguratorServiceImp(PlaceRepository placeRepository,
                                  VisitTypeRepository visitTypeRepository,
                                  VolunteerRepository volunteerRepository, SettingsRepository settingsRepository, ConfiguratorRepository configuratorRepository) {
        this.placeRepository = placeRepository;
        this.visitTypeRepository = visitTypeRepository;
        this.volunteerRepository = volunteerRepository;
        this.settingsRepository = settingsRepository;
        this.configuratorRepository = configuratorRepository;
    }

    @Override
    public boolean isFirstAccessPending() {
        return !configuratorRepository.exists ();
    }

    @Override
    public void verifyDefaultCredentials(String nickname, String password) {
        if(!isFirstAccessPending())
            throw new IllegalStateException("Le credenziali personali sono già state impostate");
        if(!DEFAULT_NICKNAME.equals(nickname) || !DEFAULT_PASSWORD.equals(password))
            throw new IllegalArgumentException ("Credenziali di primo accesso non valide");

        defaultCredentialsValidated = true;
    }

    @Override
    public void setPersonalCredentials(String nickname, String password) {
        if (!isFirstAccessPending()) throw new IllegalStateException ("Le credenziali sono già state configurate");
        if (!defaultCredentialsValidated) throw new IllegalStateException ("Credenziali di default non ancora verificate");

        String sanitizedNickname = requireNonBlank(nickname, "Il nickname non può essere vuoto");
        String sanitizedPassword = requireNonBlank(password, "La password non può essere vuota");
        configuratorRepository.save (new Configurator (sanitizedNickname, sanitizedPassword));
        defaultCredentialsValidated = false;
    }

    @Override
    public boolean verifyLogin(String nickname, String password) {
        if (nickname == null || password == null) return false;
        if (!isFirstAccessPending()) return false;

        String normalizedNickname= nickname.trim ();
        String normalizedPassword = password.trim ();

        if (normalizedNickname.isEmpty() || normalizedPassword.isEmpty())
            return false;

        return configuratorRepository.load ()
                .map (map -> map.get(normalizedNickname))
                .map (configurator ->
                        Objects.equals (configurator.getPassword (), normalizedPassword))
                .orElse(false);
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    @Override
    public void setTerritorialScope(String scope) {
        String s = Objects.requireNonNull (scope, "scope nullo");
        if(s.isEmpty ()) throw new IllegalArgumentException ("scope vuoto");

        var current = settingsRepository.load ();
        if (current.isPresent ()){
            if (!current.get ().getTerritorialScope ().equals (s)) {
               throw new IllegalStateException (
                       "Territorial scope già definito come " + current.get ().getTerritorialScope () + "e non è modificabile");
            }
            return;
        }

        int DEFAULT_MAXPARTICIPANTS = 15;

        settingsRepository.save(new SystemSettings(scope, DEFAULT_MAXPARTICIPANTS, new HashMap<> ()));
    }

    @Override
    public void setMaxPeoplePerSubscription(int max) {
        if(max < 1)  throw new IllegalArgumentException ("max people per subscription deve essere positiva");

        var current = settingsRepository.load ()
                .orElseThrow (() -> new IllegalStateException ("SystemSettings non ancora inizializzati: impostare prima l'ambito territoriale"));

        //Idempotenza
        if (current.getMaxPeoplePerSubscription () == max) return;

        current.setMaxPeoplePerSubscription (max);
        settingsRepository.save(current);

    }

    @Override
    public String addPlace(String name, String description, String location) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException ("Il nome del luogo non può essere nullo");

        if (placeRepository.findById (name).isPresent())
            throw new IllegalArgumentException ("Un luogo con questo nome esiste già");

        Place place = new Place(name, description, location);
        place.setVisits (new ArrayList<> ());
        placeRepository.save(place);
        return "Luogo inserito: " + name;
    }

    @Override
    public String addVisitType(String placeID, String title, String description, String meetLocation, List<TimeSlot> schedules, boolean ticketRequired, int minParticipants, int maxParticipants, LocalDate validFrom, LocalDate validTo) {
        if(placeID == null || placeID.isBlank())
            throw  new IllegalArgumentException ("Identificativo luogo non valido");

        if (title == null || title.isBlank())
            throw new IllegalArgumentException ("Titolo visita non valido");

        if(minParticipants <= 0 || maxParticipants <= 0 || minParticipants > maxParticipants)
            throw new  IllegalArgumentException ("Numero partecipanti non valido");

        Place place = placeRepository.findById (placeID)
                .orElseThrow (() -> new IllegalArgumentException ("Luogo non trovato"));
        if(visitTypeRepository.findById (title).isPresent())
            throw new IllegalArgumentException ("Esiste già una visita con questo titolo");

        List<TimeSlot> copySchedule = schedules == null ? new ArrayList<>() : new ArrayList<> (schedules);
        VisitType visitType = new VisitType (
                title,
                description,
                meetLocation,
                validFrom,
                validTo,
                copySchedule,
                ticketRequired,
                minParticipants,
                maxParticipants,
                place,
                new ArrayList<> ()
        );
        visitTypeRepository.save(visitType);
        if (place.getVisits () == null) place.setVisits (new ArrayList<> ());
        place.addVisit (visitType);
        placeRepository.save(place);
        return "Visita inserita: " + title;
    }

    @Override
    public void addVolunteer(String nickname) {
        if(nickname == null || nickname.isBlank()) throw new IllegalArgumentException ("Nickname non valido");
        if(volunteerRepository.findByNickname (nickname).isPresent()) throw new IllegalArgumentException ("Nickname già presente");
        Volunteer volunteer = new  Volunteer (nickname);
        volunteer.setVisitsAttending (new ArrayList<> ());
        volunteerRepository.save(volunteer);
    }

    @Override
    public void linkVOlunteerToVisit(String nickname, String visitTypeId) {
        //ricerca in repository del volontario associato al nickname
        Volunteer volunteer = volunteerRepository.findByNickname (nickname)
                .orElseThrow ( () -> new IllegalArgumentException ("Volontario non trovato"));
        //ricerca in repository della visita associata al titolo
        VisitType visitType = visitTypeRepository.findById (visitTypeId)
                .orElseThrow(() -> new IllegalArgumentException ("Visita non trovata"));

        //creazione delle liste per evitare errori in caso di liste non presenti
        if (volunteer.getVisitsAttending () == null) volunteer.setVisitsAttending (new ArrayList<> ());
        if (visitType.getGuides () == null) visitType.setGuides (new ArrayList<> ());

        //link visitType e volunteer
        if (!volunteer.getVisitsAttending ().contains (visitType))
            volunteer.addVisit(visitType);
        if (!visitType.getGuides ().contains (volunteer))
            visitType.addGuide (volunteer);

        //salvataggio in repository dei valori modificati
        volunteerRepository.save(volunteer);
        visitTypeRepository.save(visitType);
    }

    @Override
    public List<PlaceDTO> listPlace() {
        return placeRepository.findAll ().stream ()
                .map (DTOMapper::placeToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<VisitTypeDTO> listVisitTypeByPlace(String placeId) {
        if(placeId == null || placeId.isBlank())
            throw new IllegalArgumentException ("Identificativo luogo non valido");

        if (!placeRepository.findById (placeId).isPresent())
            throw new IllegalArgumentException ("Nessun luogo trovato con id %s".formatted(placeId));

        return visitTypeRepository.findByPlace (placeId).stream ()
                .map (DTOMapper::visitTypeToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<VisitTypeDTO> listVisitType(){
        return visitTypeRepository.findAll ().stream ()
                .map (DTOMapper::visitTypeToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<VolunteerDTO> listVolunteerWVisitType() {
        return volunteerRepository.findAll ().stream ()
                .map (DTOMapper::volunteerToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void setBlackoutDates(List<LocalDate> dates) {
        var current = settingsRepository.load ()
                .orElseThrow ( () -> new IllegalStateException ("System settings non ancora inizializzati"));

        List<LocalDate> sanitized = sanitizedExcludedDates(dates);
        ensureAllowedMonth(sanitized);
        current.setExcludedDates (sanitized);
        settingsRepository.save(current);
    }

    private List<LocalDate> sanitizedExcludedDates(List<LocalDate> dates) {
        if (dates == null){
            return List.of();
        }
        return dates.stream()
                .filter(Objects::nonNull)
                .distinct ()
                .sorted ()
                .collect (Collectors.toCollection(ArrayList::new));

    }
    private void ensureAllowedMonth(List<LocalDate> dates) {
        if (dates == null) return;

        YearMonth allowedMonth = ExcludedDatePolicy.allowedMonth(LocalDate.now());
        boolean hasInvalid = dates.stream()
                .anyMatch (date -> !YearMonth.from(date).equals(allowedMonth));
        if (hasInvalid) {
            throw new IllegalArgumentException(
                    "Le date precluse devono appartenere al mese " + allowedMonth + "."
            );
        }
    }




}
