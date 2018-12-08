package com.ggp.solvers.cfr.utils;

import com.ggp.IAction;
import com.ggp.IInformationSet;
import com.ggp.IPercept;

import java.util.List;
import java.util.Objects;

public class RandomNodeIS implements IInformationSet {
    private final int depth;
    private final List<IAction> legalActions;

    public RandomNodeIS(int depth, List<IAction> legalActions) {
        this.depth = depth;
        this.legalActions = legalActions;
    }

    @Override
    public IInformationSet next(IAction a) {
        return null;
    }

    @Override
    public IInformationSet applyPercept(IPercept p) {
        return null;
    }

    @Override
    public List<IAction> getLegalActions() {
        return legalActions;
    }

    @Override
    public boolean isLegal(IAction a) {
        return false;
    }

    @Override
    public boolean isValid(IPercept p) {
        return false;
    }

    @Override
    public int getOwnerId() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RandomNodeIS that = (RandomNodeIS) o;
        return depth == that.depth &&
                Objects.equals(legalActions, that.legalActions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(depth, legalActions);
    }
}
