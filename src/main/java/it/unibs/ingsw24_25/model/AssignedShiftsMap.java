package it.unibs.ingsw24_25.model;

import jakarta.persistence.*;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Entity
public class AssignedShiftsMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "`month`")
    private YearMonth month;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "assigned_shifts", joinColumns = @JoinColumn(name = "map_id"))
    private List<AssignedShift> shifts = new ArrayList<>();

    protected AssignedShiftsMap() {}

    public AssignedShiftsMap(YearMonth month) {
        this.month = month;
    }

    public List<AssignedShift> getShifts() {
        return shifts;
    }

    public void setShifts(List<AssignedShift> shifts) {
        this.shifts = shifts;
    }
}
