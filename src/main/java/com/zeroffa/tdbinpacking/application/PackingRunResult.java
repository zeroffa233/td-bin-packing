package com.zeroffa.tdbinpacking.application;

import com.zeroffa.tdbinpacking.model.PackingResult;

import java.time.Instant;

public class PackingRunResult {

    private final PackingCase packingCase;
    private final PackingResult packingResult;
    private final PackingPreparation packingPreparation;
    private final Instant startedAt;
    private final long durationNanos;

    public PackingRunResult(PackingCase packingCase,
                            PackingResult packingResult,
                            PackingPreparation packingPreparation,
                            Instant startedAt,
                            long durationNanos) {
        this.packingCase = packingCase;
        this.packingResult = packingResult;
        this.packingPreparation = packingPreparation;
        this.startedAt = startedAt;
        this.durationNanos = durationNanos;
    }

    public PackingCase getPackingCase() {
        return packingCase;
    }

    public PackingResult getPackingResult() {
        return packingResult;
    }

    public PackingPreparation getPackingPreparation() {
        return packingPreparation;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public long getDurationNanos() {
        return durationNanos;
    }

    public double getDurationMillis() {
        return durationNanos / 1_000_000.0D;
    }
}
