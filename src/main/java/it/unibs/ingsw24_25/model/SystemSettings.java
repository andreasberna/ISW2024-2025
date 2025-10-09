package it.unibs.ingsw24_25.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class SystemSettings {

    private String territorialScope;
    private int MaxPeoplePerSubscription;
    private List<LocalDate> excludedDates;

    public SystemSettings(String territorialScope, int maxPeoplePerSubscription,List<LocalDate> excludedDates) {
        this.territorialScope = territorialScope;
        this.MaxPeoplePerSubscription = maxPeoplePerSubscription;
        setExcludedDates(excludedDates);
    }

    public String getTerritorialScope() {
        return territorialScope;
    }
    public void setTerritorialScope(String territorialScope) {
        this.territorialScope = territorialScope;
    }
    public int getMaxPeoplePerSubscription() {
        return MaxPeoplePerSubscription;
    }
    public void setMaxPeoplePerSubscription(int maxPeoplePerSubscription) {
        this.MaxPeoplePerSubscription = maxPeoplePerSubscription;
    }
    public List<LocalDate> getExcludedDates() {
        return excludedDates == null ? List.of() : Collections.unmodifiableList(excludedDates);
    }
    public void setExcludedDates(List<LocalDate> excludedDates) {
        if (excludedDates == null) {this.excludedDates = new ArrayList<>();}
        else this.excludedDates = new ArrayList<> (excludedDates);
    }

}
