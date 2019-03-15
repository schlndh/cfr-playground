package com.ggp.players.continual_resolving.cfrd.AugmentedIS;

import com.ggp.IAction;
import com.ggp.IInformationSet;
import com.ggp.IPercept;

import java.util.List;
import java.util.Objects;

public class CFRDAugmentedIS implements IInformationSet {
    private final int opponentId;
    private final IInformationSet opponentsLastIs;
    private final IAction opponentsLastAction;


    public CFRDAugmentedIS(int opponentId, IInformationSet opponentsLastIs, IAction opponentsLastAction) {
        this.opponentId = opponentId;
        this.opponentsLastIs = opponentsLastIs;
        this.opponentsLastAction = opponentsLastAction;
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
        return null;
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
        return opponentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CFRDAugmentedIS that = (CFRDAugmentedIS) o;
        return opponentId == that.opponentId &&
                Objects.equals(opponentsLastIs, that.opponentsLastIs) &&
                Objects.equals(opponentsLastAction, that.opponentsLastAction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(opponentId, opponentsLastIs, opponentsLastAction);
    }

    @Override
    public String toString() {
        return "CFRDAIS{" + Objects.toString(opponentsLastIs) + " -> " + Objects.toString(opponentsLastAction) + "}";
    }

    public IInformationSet getOpponentsLastIs() {
        return opponentsLastIs;
    }

    public IAction getOpponentsLastAction() {
        return opponentsLastAction;
    }
}
