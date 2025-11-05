package it.unibs.ingsw24_25.repository;

import it.unibs.ingsw24_25.model.MonthlyAvailability;
import it.unibs.ingsw24_25.model.MonthlyVisitPlan;
import it.unibs.ingsw24_25.model.PlannedVisit;
import it.unibs.ingsw24_25.model.TimeSlot;
import it.unibs.ingsw24_25.util.JSONSupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.YearMonth;
import java.util.*;

public class JSONMonthlyVisitPlanRepository implements MonthlyVisitPlanRepository {

    private final Path file;
    private final Map<YearMonth, MonthlyVisitPlan> cache = new HashMap<> ();

    public JSONMonthlyVisitPlanRepository(Path file) {
        this.file = file;
        init();
    }

    private void init() {
        try {
            Files.createDirectories(file.getParent());
            if (Files.exists(file) && Files.size(file) > 0) {
                String json = Files.readString(file);
                Map<String, MonthlyVisitPlan> loaded = JSONSupport.deserializeMonthlyVisitPlanMap(json);
                loaded.forEach((key, plan) -> {
                    if (plan != null) {
                        YearMonth month = YearMonth.parse(key);
                        if (plan.getTargetMonth() == null) {
                            plan.setTargetMonth(month);
                        }
                        cache.put(month, copyPlan(plan));
                    }
                });
            }
        } catch (IOException e) {
            System.err.println("Warn: impossibile leggere " + file + " -> cache piani vuota");
        }
    }
    private void persist() {
        try {
            Map<String, MonthlyVisitPlan> serializable = new HashMap<>();
            cache.forEach((month, plan) -> serializable.put(month.toString(), copyPlan(plan)));
            String json = JSONSupport.serializeMonthlyVisitPlanMap(serializable);
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, json);
            Files.move(tmp, file,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Persistenza piani mensili fallita su " + file, e);
        }
    }

    @Override
    public Optional<MonthlyVisitPlan> findByMonth(YearMonth month) {
        Objects.requireNonNull(month, "Il mese non può essere nullo");
        return Optional.ofNullable (cache.get(month)).map (this::copyPlan);
    }

    @Override
    public List<MonthlyVisitPlan> findAll() {
        return cache.values().stream().map (this::copyPlan).toList ();
    }

    @Override
    public void save(MonthlyVisitPlan plan) {
        Objects.requireNonNull(plan, "Il piano non può essere nullo");
        YearMonth month = Objects.requireNonNull(plan.getTargetMonth(), "Il mese di riferimento è obbligatorio");
        cache.put(month, copyPlan(plan));
        persist();
    }

    @Override
    public void deleteByMonth(YearMonth month) {
        Objects.requireNonNull(month, "Il mese non può essere nullo");
        if (cache.remove(month) != null) {
            persist();
        }
    }

    @Override
    public void removePlannedVisitsByVisitType(String visitTypeId) {
        if (visitTypeId == null) {
            return;
        }
        boolean modified = false;
        for (MonthlyVisitPlan plan : cache.values()) {
            modified |= plan.removeVisitsByVisitType(visitTypeId);
        }
        if (modified) {
            persist();
        }
    }

    @Override
    public void removePlannedVisitsByVisitTypes(Iterable<String> visitTypeIds) {
        if (visitTypeIds == null) {
            return;
        }
        boolean modified = false;
        for (MonthlyVisitPlan plan : cache.values()) {
            modified |= plan.removeVisitsByVisitTypes(visitTypeIds);
        }
        if (modified) {
            persist();
        }
    }

    @Override
    public void removeVolunteerAssignments(String volunteerId) {
        if (volunteerId == null) {
            return;
        }
        boolean modified = false;
        for (MonthlyVisitPlan plan : cache.values()) {
            modified |= plan.removeVolunteerAssignments(volunteerId);
        }
        if (modified) {
            persist();
        }
    }

    private MonthlyVisitPlan copyPlan(MonthlyVisitPlan plan) {
        return new MonthlyVisitPlan(
                plan.getTargetMonth(),
                plan.getPhase(),
                plan.getAvailabilityWindowClosedOn(),
                copyPlannedVisits(plan.getPlannedVisits()),
                copyAvailabilitySnapshots(plan.getAvailabilitySnapshots())
        );
    }

    private List<PlannedVisit> copyPlannedVisits(List<PlannedVisit> visits) {
        if (visits == null || visits.isEmpty()) {
            return Collections.emptyList();
        }
        List<PlannedVisit> copy = new ArrayList<>();
        for (PlannedVisit visit : visits) {
            if (visit == null) {
                continue;
            }
            TimeSlot slot = visit.getTimeSlot();
            if (slot == null) {
                continue;
            }
            TimeSlot clonedSlot = new TimeSlot (slot.getDay(), slot.getStartTime(), slot.getDuration());
            PlannedVisit cloned = new PlannedVisit(visit.getDate(), clonedSlot, visit.getVisitTypeId(),
                    visit.isProposable(), visit.getAssignedVolunteerIds());
            copy.add(cloned);
        }
        return copy;
    }

    private Map<String, MonthlyAvailability> copyAvailabilitySnapshots(Map<String, MonthlyAvailability> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, MonthlyAvailability> copy = new HashMap<>();
        snapshots.forEach((nickname, availability) -> {
            if (nickname != null && availability != null) {
                availability.ensureConsistency();
                copy.put(nickname, new MonthlyAvailability(
                        availability.getReferenceMonth(),
                        EnumSet.copyOf(availability.getPreferredDays()),
                        availability.getWeeklyFrequency(),
                        availability.getSubmittedOn(),
                        availability.isSnapshot(),
                        availability.getSnapshotCapturedOn()
                ));
            }
        });
        return copy;
    }
}
