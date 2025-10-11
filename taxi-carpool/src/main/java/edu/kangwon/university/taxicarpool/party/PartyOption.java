package edu.kangwon.university.taxicarpool.party;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class PartyOption {

    @Column(name = "same_gender_only")
    private boolean sameGenderOnly;

    @Column(name = "cost_share_before_drop_off")
    private boolean costShareBeforeDropOff;

    @Column(name = "quiet_mode")
    private boolean quietMode;

    @Column(name = "destination_change_5minutes")
    private boolean destinationChangeIn5Minutes;

    public PartyOption() {}

    public PartyOption(boolean sameGenderOnly,
        boolean costShareBeforeDropOff,
        boolean quietMode,
        boolean destinationChangeIn5Minutes) {
        this.sameGenderOnly = sameGenderOnly;
        this.costShareBeforeDropOff = costShareBeforeDropOff;
        this.quietMode = quietMode;
        this.destinationChangeIn5Minutes = destinationChangeIn5Minutes;
    }

    public boolean isSameGenderOnly() { return sameGenderOnly; }
    public boolean isCostShareBeforeDropOff() { return costShareBeforeDropOff; }
    public boolean isQuietMode() { return quietMode; }
    public boolean isDestinationChangeIn5Minutes() { return destinationChangeIn5Minutes; }

    public void setSameGenderOnly(boolean sameGenderOnly) { this.sameGenderOnly = sameGenderOnly; }
    public void setCostShareBeforeDropOff(boolean costShareBeforeDropOff) { this.costShareBeforeDropOff = costShareBeforeDropOff; }
    public void setQuietMode(boolean quietMode) { this.quietMode = quietMode; }
    public void setDestinationChangeIn5Minutes(boolean destinationChangeIn5Minutes) { this.destinationChangeIn5Minutes = destinationChangeIn5Minutes; }
}
