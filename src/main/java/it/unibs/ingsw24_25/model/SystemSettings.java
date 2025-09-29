package it.unibs.ingsw24_25.model;

public class SystemSettings {

    private String territorialScope;
    private int MaxPeoplePerSubscription;

    public SystemSettings(String territorialScope, int MaxPeoplePerSubscription) {
        this.territorialScope = territorialScope;
        this.MaxPeoplePerSubscription = MaxPeoplePerSubscription;
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
    public void setMaxPeoplePerSubscription(int MaxPeoplePerSubscription) {
        this.MaxPeoplePerSubscription = MaxPeoplePerSubscription;
    }
}
