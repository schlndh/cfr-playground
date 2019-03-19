package com.ggp.players.continual_resolving.cfrd;

import com.ggp.IAction;
import com.ggp.IInformationSet;
import com.ggp.IPercept;
import com.ggp.players.continual_resolving.cfrd.actions.FollowAction;
import com.ggp.players.continual_resolving.cfrd.actions.TerminateAction;
import com.ggp.players.continual_resolving.cfrd.percepts.ISSelectedPercept;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class OpponentsChoiceIS implements IInformationSet {
    private static final long serialVersionUID = 1L;
    private int owner;
    private IInformationSet followIS;


    public OpponentsChoiceIS(int owner, IInformationSet followIS) {
        this.owner = owner;
        this.followIS = followIS;
    }

    @Override
    public IInformationSet next(IAction a) {
        if (!isLegal(a)) return null;
        if (a.getClass() == TerminateAction.class) return null;
        return followIS;
    }

    @Override
    public IInformationSet applyPercept(IPercept p) {
        if (!isValid(p)) return null;
        ISSelectedPercept per = (ISSelectedPercept) p;
        return per.getInformationSet();
    }

    @Override
    public List<IAction> getLegalActions() {
        return Arrays.asList(FollowAction.instance, TerminateAction.instance);
    }

    @Override
    public boolean isLegal(IAction a) {
        return (a != null && (a.getClass() == TerminateAction.class || a.getClass() == FollowAction.class));
    }

    @Override
    public boolean isValid(IPercept p) {
        return p != null && p.getClass() == ISSelectedPercept.class;
    }

    @Override
    public int getOwnerId() {
        return owner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OpponentsChoiceIS that = (OpponentsChoiceIS) o;
        return owner == that.owner &&
                Objects.equals(followIS, that.followIS);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, followIS);
    }

    @Override
    public String toString() {
        return "CFRDOpponentsChoiceIS{" + followIS.toString() + "}";
    }
}
