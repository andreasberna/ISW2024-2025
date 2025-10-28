package it.unibs.ingsw24_25.model;

import it.unibs.ingsw24_25.util.AvailabilitySubmissionPolicy;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.*;

public final class MonthlyAvailability {
    private YearMonth referenceMonth;
    private EnumSet<DayOfWeek> preferredDays = EnumSet.noneOf(DayOfWeek.class);
    private int weeklyFrequency;
    private LocalDate submittedOn;
    private boolean snapshot;
    private LocalDate snapshotCapturedOn;


    public MonthlyAvailability(YearMonth referenceMonth, Set<DayOfWeek> preferredDays, int weeklyFrequency, LocalDate submittedOn){
        this(referenceMonth, preferredDays, weeklyFrequency, submittedOn, false, null);
    }

    public MonthlyAvailability(YearMonth referenceMonth, Set<DayOfWeek> preferredDays,
                               int weeklyFrequency, LocalDate submittedOn,
                               boolean snapshot, LocalDate snapshotCapturedOn) {
        this.referenceMonth = referenceMonth;
        setPreferredDays(preferredDays);
        this.weeklyFrequency = weeklyFrequency;
        this.submittedOn = submittedOn;
        this.snapshot = snapshot;
        this.snapshotCapturedOn = snapshotCapturedOn;
    }


    public LocalDate getSubmittedOn() {
        return submittedOn;
    }
    public YearMonth getReferenceMonth() {
        return referenceMonth;
    }
    public EnumSet<DayOfWeek> getPreferredDays() {
        return preferredDays;
    }
    public int getWeeklyFrequency() {
        return weeklyFrequency;
    }
    public boolean isSnapshot() {
        return snapshot;
    }
    public LocalDate getSnapshotCapturedOn() {
        return snapshotCapturedOn;
    }

    public MonthlyAvailability createSnapshot(LocalDate capturedOn){
        LocalDate captureDate = capturedOn == null ? LocalDate.now () : capturedOn;
        MonthlyAvailability copy = new MonthlyAvailability (referenceMonth, preferredDays, weeklyFrequency, submittedOn, true, captureDate);
        copy.ensureConsistency ();
        return copy;
    }
    public List<LocalDate> resolveAvailableDates() {
        ensureConsistency ();
        List<LocalDate> dates = new ArrayList<> ();
        WeekFields weekFields = WeekFields.ISO;
        Map<Integer, Integer> weeklyCounts = new HashMap<> ();
        LocalDate current = referenceMonth.atDay(1);
        LocalDate end = referenceMonth.atEndOfMonth();
        while (!current.isAfter (end)) {
            if (preferredDays.contains(current.getDayOfWeek())) {
                int week = current.get (weekFields.weekOfWeekBasedYear());
                int count = weeklyCounts.getOrDefault(week, 0) + 1;
                if (count < weeklyFrequency) {
                    dates.add (current);
                    weeklyCounts.put (week, count + 1);
                }
            }
            current = current.plusDays(1);
        }
        return List.copyOf(dates);
    }

    public void validateWindow(LocalDate today){
        if (!AvailabilitySubmissionPolicy.isWindowOpen(referenceMonth, today))
            throw new IllegalStateException("La finestra per il mese " + referenceMonth + " è chiusa");
    }

    private void setPreferredDays(Set<DayOfWeek> preferredDays) {
        Objects.requireNonNull(preferredDays);
        if (preferredDays.isEmpty()) throw new IllegalArgumentException ("Preferred Days non può essere vuoto");

        this.preferredDays = EnumSet.copyOf (preferredDays);
    }

    private int validateWeeklyFrequency(int weeklyFrequency) {
        if (weeklyFrequency <= 0)
            throw new IllegalArgumentException ("Le frequenze settimanali devono essere un intero positive");
        if (weeklyFrequency > preferredDays.size ())
            throw new IllegalArgumentException ("la frequenza di giorni non può superare il numero di giorni preferiti");
        if (weeklyFrequency > DayOfWeek.values ().length)
            throw new IllegalArgumentException ("la frequenza non può superare i 7 giorni della settimana");

        return weeklyFrequency;
    }

    public void ensureConsistency() {
        setPreferredDays(preferredDays);
        this.weeklyFrequency = validateWeeklyFrequency(weeklyFrequency);
        this.referenceMonth = Objects.requireNonNull(referenceMonth);
        this.submittedOn = Objects.requireNonNull(submittedOn);
        if (snapshot)
            this.snapshotCapturedOn = Objects.requireNonNull(snapshotCapturedOn);
    }
}
