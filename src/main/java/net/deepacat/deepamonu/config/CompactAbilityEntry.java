package net.deepacat.deepamonu.config;

public class CompactAbilityEntry {
    public String abilityName;
    public String formatString;
    public int xOffset;
    public int yOffset;
    public float textScale;
    public int alignment;

    public CompactAbilityEntry() {
        this.abilityName = "";
        this.formatString = "%cds";
        this.xOffset = 0;
        this.yOffset = 45;
        this.textScale = 1.0f;
        this.alignment = 0;
    }

    public CompactAbilityEntry(String abilityName, String formatString) {
        this.abilityName = abilityName;
        this.formatString = formatString;
        this.xOffset = 0;
        this.yOffset = 45;
        this.textScale = 1.0f;
        this.alignment = 0;
    }

    public CompactAbilityEntry(String abilityName, String formatString, int xOffset, int yOffset, float textScale, int alignment) {
        this.abilityName = abilityName;
        this.formatString = formatString;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.textScale = textScale;
        this.alignment = alignment;
    }

    public CompactAbilityEntry(CompactAbilityEntry other) {
        this.abilityName = other.abilityName;
        this.formatString = other.formatString;
        this.xOffset = other.xOffset;
        this.yOffset = other.yOffset;
        this.textScale = other.textScale;
        this.alignment = other.alignment;
    }
}
