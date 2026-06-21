package it.unibs.ingsw24_25.repository;

import it.unibs.ingsw24_25.model.PlannedVisit;
import it.unibs.ingsw24_25.model.VisitStatus;
import it.unibs.ingsw24_25.model.Volunteer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PlannedVisitRepository extends JpaRepository<PlannedVisit, Long> {
    List<PlannedVisit> findByVisitDateAndStatus(LocalDate date, VisitStatus status);
    List<PlannedVisit> findByStatus(VisitStatus status);
    List<PlannedVisit> findByVolunteer(Volunteer volunteer);

    @Query("SELECT pv FROM PlannedVisit pv WHERE pv.visitType.place.id = :placeId AND pv.visitDate = :date")
    List<PlannedVisit> findByPlaceIdAndVisitDate(@Param("placeId") String placeId, @Param("date") LocalDate date);

    List<PlannedVisit> findByVisitType_Id(String visitTypeId);
}
