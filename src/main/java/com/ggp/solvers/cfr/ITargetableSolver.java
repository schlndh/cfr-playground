package com.ggp.solvers.cfr;

public interface ITargetableSolver {
    /**
     * Sets current targeting for the solver
     * @param targeting targeting or null to disable targeting
     */
    void setTargeting(ISearchTargeting targeting);

    /**
     * Asks the solver if it wants targeting (eg. targeting probability {@literal >} 0)
     * @return
     */
    boolean wantsTargeting();
}
