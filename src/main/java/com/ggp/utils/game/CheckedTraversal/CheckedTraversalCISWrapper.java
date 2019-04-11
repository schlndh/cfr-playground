package com.ggp.utils.game.CheckedTraversal;

import com.ggp.IAction;
import com.ggp.ICompleteInformationState;
import com.ggp.IPercept;
import com.ggp.utils.CompleteInformationStateWrapper;

public class CheckedTraversalCISWrapper extends CompleteInformationStateWrapper {
    private static final long serialVersionUID = 1L;
    public CheckedTraversalCISWrapper(ICompleteInformationState state) {
        super(state);
    }

    @Override
    public ICompleteInformationState next(IAction a) {
        if (!state.isLegal(a)) {
            throw new RuntimeException("Invalid action " + a + " passed to state " + state);
        }
        return new CheckedTraversalCISWrapper(state.next(a));
    }

    @Override
    public Iterable<IPercept> getPercepts(IAction a) {
        if (!state.isLegal(a)) {
            throw new RuntimeException("Invalid action " + a + " passed to state " + state);
        }
        return state.getPercepts(a);
    }

    @Override
    public String toString() {
        return "CheckedTraversal{" +
                    state +
                '}';
    }
}
