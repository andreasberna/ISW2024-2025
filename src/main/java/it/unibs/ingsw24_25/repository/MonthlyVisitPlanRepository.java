package it.unibs.ingsw24_25.repository;

import it.unibs.ingsw24_25.model.MonthlyVisitPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.Optional;

@Repository
public interface MonthlyVisitPlanRepository extends JpaRepository<MonthlyVisitPlan, Long> {
    Optional<MonthlyVisitPlan> findByTargetMonth(YearMonth targetMonth);
}
