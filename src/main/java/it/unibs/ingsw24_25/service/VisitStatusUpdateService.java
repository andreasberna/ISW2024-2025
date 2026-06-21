package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.model.PlannedVisit;
import it.unibs.ingsw24_25.model.VisitBooking;
import it.unibs.ingsw24_25.model.VisitStatus;
import it.unibs.ingsw24_25.model.VisitType;
import it.unibs.ingsw24_25.repository.PlannedVisitRepository;
import it.unibs.ingsw24_25.repository.VisitTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class VisitStatusUpdateService {
    private static final Logger log = LoggerFactory.getLogger(VisitStatusUpdateService.class);

    private final PlannedVisitRepository plannedVisitRepository;
    private final VisitTypeRepository visitTypeRepository;

    public VisitStatusUpdateService(PlannedVisitRepository plannedVisitRepository, VisitTypeRepository visitTypeRepository) {
        this.plannedVisitRepository = plannedVisitRepository;
        this.visitTypeRepository = visitTypeRepository;
    }

    @Scheduled(cron = "0 0 1 * * *") // Runs every day at 1:00 AM
    @Transactional
    public void updateVisitStatuses() {
        LocalDate threeDaysFromNow = LocalDate.now().plusDays(3);
        log.info("Aggiornamento stato visite per la data {}", threeDaysFromNow);

        List<PlannedVisit> visitsToConfirmOrCancel = plannedVisitRepository.findByVisitDateAndStatus(threeDaysFromNow, VisitStatus.PROPOSTA);
        log.info("Trovate {} visite da valutare", visitsToConfirmOrCancel.size());

        for (PlannedVisit visit : visitsToConfirmOrCancel) {
            VisitType visitType = visit.getVisitType();
            int totalParticipants = visit.countBookedParticipants();

            if (totalParticipants >= visitType.getMinParticipants()) {
                visit.setStatus(VisitStatus.CONFERMATA);
                log.info("Visita {} CONFERMATA ({} partecipanti >= min {})",
                        visit.getId(), totalParticipants, visitType.getMinParticipants());
            } else {
                visit.setStatus(VisitStatus.CANCELLATA);
                log.info("Visita {} CANCELLATA ({} partecipanti < min {})",
                        visit.getId(), totalParticipants, visitType.getMinParticipants());
            }
            plannedVisitRepository.save(visit);
        }
    }

    @Transactional
    public void checkAndCompleteVisit(Long visitId) {
        PlannedVisit visit = plannedVisitRepository.findById(visitId).orElse(null);
        if (visit != null && visit.getStatus() == VisitStatus.PROPOSTA) {
            VisitType visitType = visit.getVisitType();
            int totalParticipants = visit.countBookedParticipants();

            if (totalParticipants >= visitType.getMaxParticipants()) {
                visit.setStatus(VisitStatus.COMPLETA);
                plannedVisitRepository.save(visit);
            }
        }
    }

    @Transactional
    public void revertToProposta(Long visitId) {
        PlannedVisit visit = plannedVisitRepository.findById(visitId).orElse(null);
        if (visit != null && visit.getStatus() == VisitStatus.COMPLETA) {
            visit.setStatus(VisitStatus.PROPOSTA);
            plannedVisitRepository.save(visit);
        }
    }
}
