package com.ggp.utils.recall;

import com.ggp.IPercept;

import java.io.Serializable;
import java.util.Objects;

class PerceptListNode implements Serializable {
    private static final long serialVersionUID = 1L;
    public final PerceptListNode previous;
    public final IPercept percept;
    private final int hash;

    public PerceptListNode(PerceptListNode previous, IPercept percept) {
        this.previous = previous;
        this.percept = percept;
        this.hash = Objects.hash(previous, percept);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (this.hashCode() != o.hashCode()) return false;
        PerceptListNode that = (PerceptListNode) o;
        return Objects.equals(percept, that.percept) &&Objects.equals(previous, that.previous);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    public PerceptListNode getPrevious() {
        return previous;
    }

    public IPercept getPercept() {
        return percept;
    }
}
