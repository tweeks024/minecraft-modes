package com.tweeks.starwars.faction;

public enum SwFaction {
    EMPIRE,
    LIGHT,
    NEUTRAL;

    /** The faction this one attacks on sight. NEUTRAL fights nobody. */
    public SwFaction enemy() {
        return switch (this) {
            case EMPIRE -> LIGHT;
            case LIGHT -> EMPIRE;
            case NEUTRAL -> NEUTRAL;
        };
    }
}
