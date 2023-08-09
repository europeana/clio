package eu.europeana.clio.linkchecking.config;

import java.util.Arrays;

/**
 * Mode of the execution.
 */
public enum Mode {
    FULL_PROCESSING, LINK_CHECKING_ONLY;

    /**
     * Get the mode from a string.
     * <p>Empty or null strings will default to {@link Mode.FULL_PROCESSING}</p>
     * @param modeString the mode string
     * @return the mode
     */
    public static Mode getMode(String modeString){
        return Arrays.stream(Mode.values()).filter(mode -> mode.name().equals(modeString)).findAny().orElse(Mode.FULL_PROCESSING);
    }
}
