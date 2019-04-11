package com.ggp.utils.game.CheckedTraversal;

import com.ggp.IAction;
import com.ggp.IInformationSet;
import com.ggp.IPercept;
import com.ggp.utils.InformationSetWrapper;

public class CheckedTraversalISWrapper extends InformationSetWrapper {
    private static final long serialVersionUID = 1L;
    public CheckedTraversalISWrapper(IInformationSet infoSet) {
        super(infoSet);
    }

    @Override
    public IInformationSet next(IAction a) {
        if (!infoSet.isLegal(a)) {
            throw new RuntimeException("Invalid action " + a + " passed to IS " + infoSet);
        }
        return new CheckedTraversalISWrapper(infoSet.next(a));
    }

    @Override
    public IInformationSet applyPercept(IPercept p) {
        if (!infoSet.isValid(p)) {
            throw new RuntimeException("Invalid percept " + p + " passed to IS " + infoSet);
        }
        return new CheckedTraversalISWrapper(infoSet.applyPercept(p));
    }

    @Override
    public String toString() {
        return "CheckedTraversal{" +
                    infoSet +
                '}';
    }
}
