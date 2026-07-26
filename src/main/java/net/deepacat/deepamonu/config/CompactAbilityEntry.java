package net.deepacat.deepamonu.config;

public class CompactAbilityEntry {
    public String abilityName;
    public String formatString;

    public CompactAbilityEntry() {
        this.abilityName = "";
        this.formatString = "%cds";
    }

    public CompactAbilityEntry(String abilityName, String formatString) {
        this.abilityName = abilityName;
        this.formatString = formatString;
    }
}
