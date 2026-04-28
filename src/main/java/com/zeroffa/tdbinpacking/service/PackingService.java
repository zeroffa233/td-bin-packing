package com.zeroffa.tdbinpacking.service;

import com.zeroffa.tdbinpacking.api.PackingRequest;
import com.zeroffa.tdbinpacking.api.PackingResponse;
import com.zeroffa.tdbinpacking.api.ResponseData;
import com.zeroffa.tdbinpacking.solver.ExtremePointBinPacker;

import java.util.Collections;
import java.util.List;

public class PackingService {

    private final RequestNormalizer normalizer;
    private final GroupPackingCoordinator coordinator;
    private final ResponseAssembler responseAssembler;

    public PackingService() {
        CorePackerAdapter corePackerAdapter = new CorePackerAdapter(new ExtremePointBinPacker());
        RuntimeLegalityValidator legalityValidator = new RuntimeLegalityValidator();
        ContainerCandidateService containerCandidateService = new ContainerCandidateService();
        TraditionalMultiBoxPlanner traditionalPlanner = new TraditionalMultiBoxPlanner(corePackerAdapter, legalityValidator);
        LclRuleOrchestrator lclRuleOrchestrator = new LclRuleOrchestrator(corePackerAdapter, legalityValidator, traditionalPlanner);
        ExpressPackingPlanner expressPlanner = new ExpressPackingPlanner(
                new ItemPreprocessor(),
                containerCandidateService,
                lclRuleOrchestrator
        );
        LogisticShortcutPlanner logisticPlanner = new LogisticShortcutPlanner(containerCandidateService);

        this.normalizer = new RequestNormalizer();
        this.coordinator = new GroupPackingCoordinator(expressPlanner, logisticPlanner);
        this.responseAssembler = new ResponseAssembler();
    }

    public PackingResponse pack(PackingRequest request) {
        try {
            NormalizedRequest normalizedRequest = normalizer.normalize(request);
            RequestPackingPlan plan = coordinator.pack(normalizedRequest);
            return responseAssembler.assemble(plan, normalizedRequest.unpackJudge());
        } catch (BusinessException | IllegalArgumentException exception) {
            return new PackingResponse(1, exception.getMessage(), new ResponseData(Collections.<com.zeroffa.tdbinpacking.api.ResponseInfo>emptyList()));
        }
    }
}
