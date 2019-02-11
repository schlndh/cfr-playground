package com.ggp.players.solving;

import com.ggp.IAction;
import com.ggp.ICompleteInformationState;
import com.ggp.solvers.cfr.ISearchTargeting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ISSearchTargeting implements ISearchTargeting {
    private final ObjectTree<ActionIdxWrapper> actionPaths;
    private final List<Integer> currentTargeting;

    public ISSearchTargeting(ObjectTree<ActionIdxWrapper> actionPaths) {
        this.actionPaths = actionPaths;
        if (actionPaths != null && actionPaths.size() > 0) {
            ArrayList<Integer> targets = new ArrayList<>(actionPaths.size());
            for (ActionIdxWrapper aw: actionPaths.getPossibleSteps()) {
                targets.add(aw.getIdx());
            }
            currentTargeting = Collections.unmodifiableList(targets);
        } else {
            currentTargeting = null;
        }
    }

    @Override
    public List<Integer> target(ICompleteInformationState s) {
        return currentTargeting;
    }

    @Override
    public ISearchTargeting next(IAction a, int actionIdx) {
        if (actionPaths == null) return null;
        ObjectTree<ActionIdxWrapper> next = actionPaths.getNext(new ActionIdxWrapper(a, actionIdx));
        if (next == null || next.size() == 0) return null;
        return new ISSearchTargeting(next);
    }
}
