package com.zeroffa.tdbinpacking.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

class ContainerCandidateService {

    List<ContainerCandidate> candidatesForGroup(PackingGroup group, List<ContainerCandidate> containers) {
        return containers.stream()
                .filter(container -> isStoreCompatible(group.stores(), container.stores()))
                .sorted(Comparator.comparingInt(ContainerCandidate::sequence)
                        .thenComparingLong(ContainerCandidate::volume)
                        .thenComparing(ContainerCandidate::code))
                .collect(Collectors.toList());
    }

    private boolean isStoreCompatible(String groupStores, String containerStores) {
        return groupStores == null || containerStores == null || Objects.equals(groupStores, containerStores);
    }
}
