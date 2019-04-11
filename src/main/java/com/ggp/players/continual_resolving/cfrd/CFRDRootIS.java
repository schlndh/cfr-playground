package com.ggp.players.continual_resolving.cfrd;

import com.ggp.IAction;
import com.ggp.IInformationSet;
import com.ggp.IPercept;
import com.ggp.players.continual_resolving.cfrd.percepts.ISSelectedPercept;

import java.util.List;
import java.util.Objects;

public class CFRDRootIS implements IInformationSet {
    private static final long serialVersionUID = 1L;
    private int owner;
    private CFRDGadgetRoot root;

    public CFRDRootIS(int owner, CFRDGadgetRoot root) {
        this.owner = owner;
        this.root = root;
    }

    @Override
    public IInformationSet next(IAction a) {
        return this;
    }

    @Override
    public IInformationSet applyPercept(IPercept p) {
        ISSelectedPercept per = (ISSelectedPercept) p;
        if (owner != root.getOpponentId()) {
            return per.getInformationSet();
        } else {
            return new OpponentsChoiceIS(root.getOpponentId(), per.getInformationSet());
        }
    }

    @Override
    public List<IAction> getLegalActions() {
        return root.getLegalActions();
    }

    @Override
    public boolean isLegal(IAction a) {
        return root.isLegal(a);
    }

    @Override
    public boolean isValid(IPercept p) {
        if (p == null || p.getClass() != ISSelectedPercept.class) return false;
        ISSelectedPercept per = (ISSelectedPercept) p;
        if (per.getInformationSet().getOwnerId() != owner) return false;
        return true;
    }

    @Override
    public int getOwnerId() {
        return owner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CFRDRootIS that = (CFRDRootIS) o;
        return owner == that.owner &&
                Objects.equals(root, that.root);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, root);
    }
}
