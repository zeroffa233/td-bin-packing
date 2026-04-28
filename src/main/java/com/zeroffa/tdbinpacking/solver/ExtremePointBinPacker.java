package com.zeroffa.tdbinpacking.solver;

import com.zeroffa.tdbinpacking.model.AxisAlignedBox;
import com.zeroffa.tdbinpacking.model.ContainerBox;
import com.zeroffa.tdbinpacking.model.ExtremePoint;
import com.zeroffa.tdbinpacking.model.Item;
import com.zeroffa.tdbinpacking.model.Orientation;
import com.zeroffa.tdbinpacking.model.PackingResult;
import com.zeroffa.tdbinpacking.model.Placement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ExtremePointBinPacker {

    private static final Comparator<ExtremePoint> EXTREME_POINT_ORDER = Comparator
            .comparingInt(ExtremePoint::getZ)
            .thenComparingInt(ExtremePoint::getY)
            .thenComparingInt(ExtremePoint::getX);

    public PackingResult pack(ContainerBox containerBox, List<Item> inputItems) {
        List<Item> sortedItems = inputItems.stream()
                .sorted(Comparator.comparingLong(Item::volume).reversed().thenComparing(Item::getId))
                .collect(Collectors.toList());

        List<Placement> placedItems = new ArrayList<>();
        List<Item> unplacedItems = new ArrayList<>();
        List<ExtremePoint> extremePoints = new ArrayList<>();
        extremePoints.add(new ExtremePoint(0, 0, 0));

        for (int index = 0; index < sortedItems.size(); index++) {
            Item item = sortedItems.get(index);
            Candidate bestCandidate = findBestCandidate(item, containerBox, placedItems, extremePoints);
            if (bestCandidate == null) {
                unplacedItems.addAll(sortedItems.subList(index, sortedItems.size()));
                break;
            }

            placedItems.add(bestCandidate.placement());
            extremePoints.remove(bestCandidate.extremePoint());
            refreshExtremePoints(extremePoints, containerBox, placedItems, bestCandidate.placement());
        }

        return new PackingResult(containerBox, placedItems, unplacedItems);
    }

    private Candidate findBestCandidate(Item item,
                                        ContainerBox containerBox,
                                        List<Placement> placedItems,
                                        List<ExtremePoint> extremePoints) {
        Candidate bestCandidate = null;
        for (ExtremePoint extremePoint : extremePoints) {
            for (Orientation orientation : Orientation.generate(item)) {
                Placement placement = new Placement(item, orientation,
                        extremePoint.getX(), extremePoint.getY(), extremePoint.getZ());

                if (!fitsInside(containerBox, placement) || collidesWithAny(placement, placedItems)) {
                    continue;
                }

                int contactArea = calculateContactArea(containerBox, placement, placedItems);
                Candidate candidate = new Candidate(extremePoint, placement, contactArea);
                if (isBetter(candidate, bestCandidate)) {
                    bestCandidate = candidate;
                }
            }
        }
        return bestCandidate;
    }

    private boolean isBetter(Candidate candidate, Candidate currentBest) {
        if (currentBest == null) {
            return true;
        }
        if (candidate.contactArea() != currentBest.contactArea()) {
            return candidate.contactArea() > currentBest.contactArea();
        }

        int pointCompare = EXTREME_POINT_ORDER.compare(candidate.extremePoint(), currentBest.extremePoint());
        if (pointCompare != 0) {
            return pointCompare < 0;
        }

        Placement candidatePlacement = candidate.placement();
        Placement currentPlacement = currentBest.placement();
        if (candidatePlacement.getSizeZ() != currentPlacement.getSizeZ()) {
            return candidatePlacement.getSizeZ() < currentPlacement.getSizeZ();
        }
        if (candidatePlacement.getSizeY() != currentPlacement.getSizeY()) {
            return candidatePlacement.getSizeY() < currentPlacement.getSizeY();
        }
        return candidatePlacement.getSizeX() < currentPlacement.getSizeX();
    }

    private boolean fitsInside(ContainerBox containerBox, Placement placement) {
        return placement.getMaxX() <= containerBox.getSizeX()
                && placement.getMaxY() <= containerBox.getSizeY()
                && placement.getMaxZ() <= containerBox.getSizeZ();
    }

    private boolean collidesWithAny(Placement candidate, List<Placement> placedItems) {
        return placedItems.stream().anyMatch(placed -> overlaps(candidate, placed));
    }

    private boolean overlaps(AxisAlignedBox first, AxisAlignedBox second) {
        return first.getX() < second.getMaxX()
                && first.getMaxX() > second.getX()
                && first.getY() < second.getMaxY()
                && first.getMaxY() > second.getY()
                && first.getZ() < second.getMaxZ()
                && first.getMaxZ() > second.getZ();
    }

    private int calculateContactArea(ContainerBox containerBox, Placement candidate, List<Placement> placedItems) {
        int totalContactArea = 0;
        if (candidate.getX() == 0) {
            totalContactArea += candidate.getSizeY() * candidate.getSizeZ();
        }
        if (candidate.getY() == 0) {
            totalContactArea += candidate.getSizeX() * candidate.getSizeZ();
        }
        if (candidate.getZ() == 0) {
            totalContactArea += candidate.getSizeX() * candidate.getSizeY();
        }

        if (candidate.getMaxX() == containerBox.getSizeX()) {
            totalContactArea += candidate.getSizeY() * candidate.getSizeZ();
        }
        if (candidate.getMaxY() == containerBox.getSizeY()) {
            totalContactArea += candidate.getSizeX() * candidate.getSizeZ();
        }
        if (candidate.getMaxZ() == containerBox.getSizeZ()) {
            totalContactArea += candidate.getSizeX() * candidate.getSizeY();
        }

        for (Placement placed : placedItems) {
            if (candidate.getX() == placed.getMaxX() || candidate.getMaxX() == placed.getX()) {
                totalContactArea += overlapOnYAndZ(candidate, placed);
            }
            if (candidate.getY() == placed.getMaxY() || candidate.getMaxY() == placed.getY()) {
                totalContactArea += overlapOnXAndZ(candidate, placed);
            }
            if (candidate.getZ() == placed.getMaxZ() || candidate.getMaxZ() == placed.getZ()) {
                totalContactArea += overlapOnXAndY(candidate, placed);
            }
        }
        return totalContactArea;
    }

    private int overlapOnYAndZ(AxisAlignedBox first, AxisAlignedBox second) {
        return positiveOverlap(first.getY(), first.getMaxY(), second.getY(), second.getMaxY())
                * positiveOverlap(first.getZ(), first.getMaxZ(), second.getZ(), second.getMaxZ());
    }

    private int overlapOnXAndZ(AxisAlignedBox first, AxisAlignedBox second) {
        return positiveOverlap(first.getX(), first.getMaxX(), second.getX(), second.getMaxX())
                * positiveOverlap(first.getZ(), first.getMaxZ(), second.getZ(), second.getMaxZ());
    }

    private int overlapOnXAndY(AxisAlignedBox first, AxisAlignedBox second) {
        return positiveOverlap(first.getX(), first.getMaxX(), second.getX(), second.getMaxX())
                * positiveOverlap(first.getY(), first.getMaxY(), second.getY(), second.getMaxY());
    }

    private int positiveOverlap(int start1, int end1, int start2, int end2) {
        return Math.max(0, Math.min(end1, end2) - Math.max(start1, start2));
    }

    private void refreshExtremePoints(List<ExtremePoint> extremePoints,
                                      ContainerBox containerBox,
                                      List<Placement> placedItems,
                                      Placement newPlacement) {
        List<Placement> supportingPlacements = placedItems.stream()
                .filter(placement -> placement != newPlacement)
                .collect(Collectors.toList());

        Set<ExtremePoint> candidates = new LinkedHashSet<>(extremePoints);
        candidates.addAll(generateProjectedPoints(newPlacement, supportingPlacements));

        extremePoints.clear();
        candidates.stream()
                .filter(point -> isWithinContainer(point, containerBox))
                .filter(point -> !isPointInsideAny(point, placedItems))
                .sorted(EXTREME_POINT_ORDER)
                .forEach(extremePoints::add);
    }

    private Collection<ExtremePoint> generateProjectedPoints(Placement placement, List<Placement> placedItems) {
        int x1 = placement.getX();
        int y1 = placement.getY();
        int z1 = placement.getZ();
        int x2 = placement.getMaxX();
        int y2 = placement.getMaxY();
        int z2 = placement.getMaxZ();

        List<ExtremePoint> projectedPoints = new ArrayList<>(6);
        projectedPoints.add(new ExtremePoint(x2, projectY(x2, z1, placedItems), z1));
        projectedPoints.add(new ExtremePoint(x2, y1, projectZ(x2, y1, placedItems)));

        projectedPoints.add(new ExtremePoint(projectX(y2, z1, placedItems), y2, z1));
        projectedPoints.add(new ExtremePoint(x1, y2, projectZ(x1, y2, placedItems)));

        projectedPoints.add(new ExtremePoint(projectX(y1, z2, placedItems), y1, z2));
        projectedPoints.add(new ExtremePoint(x1, projectY(x1, z2, placedItems), z2));
        return projectedPoints;
    }

    private int projectX(int fixedY, int fixedZ, List<Placement> placedItems) {
        int projected = 0;
        for (Placement placement : placedItems) {
            if (containsOnSpan(fixedY, placement.getY(), placement.getMaxY())
                    && containsOnSpan(fixedZ, placement.getZ(), placement.getMaxZ())) {
                projected = Math.max(projected, placement.getMaxX());
            }
        }
        return projected;
    }

    private int projectY(int fixedX, int fixedZ, List<Placement> placedItems) {
        int projected = 0;
        for (Placement placement : placedItems) {
            if (containsOnSpan(fixedX, placement.getX(), placement.getMaxX())
                    && containsOnSpan(fixedZ, placement.getZ(), placement.getMaxZ())) {
                projected = Math.max(projected, placement.getMaxY());
            }
        }
        return projected;
    }

    private int projectZ(int fixedX, int fixedY, List<Placement> placedItems) {
        int projected = 0;
        for (Placement placement : placedItems) {
            if (containsOnSpan(fixedX, placement.getX(), placement.getMaxX())
                    && containsOnSpan(fixedY, placement.getY(), placement.getMaxY())) {
                projected = Math.max(projected, placement.getMaxZ());
            }
        }
        return projected;
    }

    private boolean containsOnSpan(int coordinate, int start, int endExclusive) {
        return coordinate >= start && coordinate < endExclusive;
    }

    private boolean isWithinContainer(ExtremePoint point, ContainerBox containerBox) {
        return point.getX() < containerBox.getSizeX()
                && point.getY() < containerBox.getSizeY()
                && point.getZ() < containerBox.getSizeZ();
    }

    private boolean isPointInsideAny(ExtremePoint point, List<Placement> placedItems) {
        for (Placement placement : placedItems) {
            if (containsOnSpan(point.getX(), placement.getX(), placement.getMaxX())
                    && containsOnSpan(point.getY(), placement.getY(), placement.getMaxY())
                    && containsOnSpan(point.getZ(), placement.getZ(), placement.getMaxZ())) {
                return true;
            }
        }
        return false;
    }

    private static class Candidate {
        private final ExtremePoint extremePoint;
        private final Placement placement;
        private final int contactArea;

        private Candidate(ExtremePoint extremePoint, Placement placement, int contactArea) {
            this.extremePoint = extremePoint;
            this.placement = placement;
            this.contactArea = contactArea;
        }

        private ExtremePoint extremePoint() {
            return extremePoint;
        }

        private Placement placement() {
            return placement;
        }

        private int contactArea() {
            return contactArea;
        }
    }
}
