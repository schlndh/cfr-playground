package com.ggp.solvers.cfr;

public interface ITargetableSolver {
    /**
     * Sets current targeting for the solver
     * @param targeting targeting or null to disable targeting
     */
    void setTargeting(ISearchTargeting targeting);
}
