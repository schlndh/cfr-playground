package com.ggp.utils.recall;

import com.ggp.IAction;
import com.ggp.IInformationSet;
import com.ggp.IPercept;
import com.ggp.utils.InformationSetWrapper;
import com.ggp.utils.OwnActionPercept;

import java.util.Objects;

public class PerfectRecallIS extends InformationSetWrapper {
    private static final long serialVersionUID = 1L;
    PerceptListNode percepts;

    public PerfectRecallIS(IInformationSet infoSet, PerceptListNode percepts) {
        super(infoSet);
        this.percepts = percepts;
    }

    private PerceptListNode getNextPerceptList(IPercept p) {
        return new PerceptListNode(percepts, p);
    }

    @Override
    public IInformationSet next(IAction a) {
        return new PerfectRecallIS(infoSet.next(a), getNextPerceptList(new OwnActionPercept(infoSet.getOwnerId(), a)));
    }

    @Override
    public IInformationSet applyPercept(IPercept p) {
        return new PerfectRecallIS(infoSet.applyPercept(p), getNextPerceptList(p));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PerfectRecallIS that = (PerfectRecallIS) o;
        return infoSet.getOwnerId() == that.infoSet.getOwnerId() && Objects.equals(percepts, that.percepts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(percepts, infoSet.getOwnerId());
    }

    public PerceptListNode getPercepts() {
        return percepts;
    }

    @Override
    public String toString() {
        return "PR{" + infoSet.toString() + "}";
    }
}
