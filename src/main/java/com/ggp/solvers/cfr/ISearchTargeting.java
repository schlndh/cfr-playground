package com.ggp.solvers.cfr;

import com.ggp.IAction;
import com.ggp.ICompleteInformationState;

import java.util.List;

public interface ISearchTargeting {
    /**
     * Target search in given state
     * @param s
     * @return List of action indices consistent with the targeting, or null if targeting should be aborted
     */
    List<Integer> target(ICompleteInformationState s);

    /**
     * Get targeting for next state
     * @param a action
     * @param actionIdx action's index
     * @return Targeting for next state, or null if targeting should be aborted
     */
    ISearchTargeting next(IAction a, int actionIdx);
}
