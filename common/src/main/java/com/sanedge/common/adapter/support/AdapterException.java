package com.sanedge.common.adapter.support;

/**
 * Kegagalan panggilan adapter yang bukan "not found" (mis. service mengembalikan
 * status non-success). Setara pembungkusan {@code <domain>_errors.ErrX} di Go.
 */
public class AdapterException extends RuntimeException {

    public AdapterException(String message) {
        super(message);
    }

    public AdapterException(String message, Throwable cause) {
        super(message, cause);
    }
}
