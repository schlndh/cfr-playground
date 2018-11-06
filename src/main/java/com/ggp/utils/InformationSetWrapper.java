package com.ggp.utils;

import com.ggp.IAction;
import com.ggp.IInformationSet;
import com.ggp.IPercept;

import java.util.List;
import java.util.Objects;

/**
 * Base class for IS wrappers, delegates most methods to the original IS.
 */
public abstract class InformationSetWrapper implements IInformationSet {
    private static final long serialVersionUID = 1L;
    protected IInformationSet infoSet;

    public InformationSetWrapper(IInformationSet infoSet) {
        this.infoSet = infoSet;
    }

    @Override
    public abstract IInformationSet next(IAction a);

    @Override
    public abstract IInformationSet applyPercept(IPercept p);

    @Override
    public List<IAction> getLegalActions() {
        return infoSet.getLegalActions();
    }

    @Override
    public boolean isLegal(IAction a) {
        return infoSet.isLegal(a);
    }

    @Override
    public boolean isValid(IPercept p) {
        return infoSet.isValid(p);
    }

    @Override
    public int getOwnerId() {
        return infoSet.getOwnerId();
    }

    public IInformationSet getOrigInfoSet() {
        return infoSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InformationSetWrapper that = (InformationSetWrapper) o;
        return Objects.equals(infoSet, that.infoSet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(infoSet);
    }
}
