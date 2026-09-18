package com.nutricoach.common.util;

import java.math.BigDecimal;

/** Formatting for measurement values shown to coaches. */
public final class Measures {

    private Measures() {}

    /**
     * A weight as a coach would write it: "72.5", not "72.50".
     *
     * <p>{@code weight_kg} is {@code numeric(5,2)}, so the stored value always
     * carries two decimals. Both the dashboard feed and the client Updates feed
     * render the same events, so they format through here to stay identical.
     */
    public static String formatKg(BigDecimal kg) {
        return kg.stripTrailingZeros().toPlainString();
    }
}
