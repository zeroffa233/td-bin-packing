package com.zeroffa.tdbinpacking.application;

import com.zeroffa.tdbinpacking.model.Item;
import com.zeroffa.tdbinpacking.model.PackingResult;
import com.zeroffa.tdbinpacking.solver.ExtremePointBinPacker;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class PackingRunner {

    private final ExtremePointBinPacker packer;
    private final PackingInputPreprocessor preprocessor;

    public PackingRunner(ExtremePointBinPacker packer) {
        this.packer = Objects.requireNonNull(packer, "packer");
        this.preprocessor = new PackingInputPreprocessor(packer);
    }

    public PackingPreparation prepareItems(List<Item> items) {
        return preprocessor.prepare(items);
    }

    public PackingRunResult run(PackingCase packingCase) {
        PackingPreparation preparation;
        try {
            preparation = prepareItems(packingCase.getItems());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Packing case '" + packingCase.getCaseId() + "' failed preprocessing: "
                    + exception.getMessage(), exception);
        }
        return runPrepared(packingCase, preparation);
    }

    public PackingRunResult runPrepared(PackingCase packingCase, PackingPreparation preparation) {
        Instant startedAt = Instant.now();
        long startNanos = System.nanoTime();
        PackingResult result = packer.pack(packingCase.getContainerBox(), preparation.getPreparedItems());
        long durationNanos = System.nanoTime() - startNanos;
        return new PackingRunResult(packingCase, result, preparation, startedAt, durationNanos);
    }

    public void runBatch(Iterable<PackingCase> packingCases, Consumer<PackingRunResult> resultConsumer) {
        for (PackingCase packingCase : packingCases) {
            resultConsumer.accept(run(packingCase));
        }
    }
}
