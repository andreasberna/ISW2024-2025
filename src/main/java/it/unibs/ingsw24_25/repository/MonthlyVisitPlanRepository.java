package it.unibs.ingsw24_25.repository;

import it.unibs.ingsw24_25.model.MonthlyVisitPlan;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface MonthlyVisitPlanRepository {
    Optional<MonthlyVisitPlan> findByMonth(YearMonth month);

    List<MonthlyVisitPlan> findAll();

    void save(MonthlyVisitPlan plan);

    void deleteByMonth(YearMonth month);

    void removePlannedVisitsByVisitType(String visitTypeId);

    void removePlannedVisitsByVisitTypes(Iterable<String> visitTypeIds);

    void removeVolunteerAssignments(String volunteerId);
}
