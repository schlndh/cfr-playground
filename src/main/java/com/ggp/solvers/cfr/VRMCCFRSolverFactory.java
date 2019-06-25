package com.ggp.solvers.cfr;

public class VRMCCFRSolverFactory extends MCCFRSolver.Factory {
    private IBaseline.IFactory baselineFactory;

    public VRMCCFRSolverFactory(IRegretMatching.IFactory rmFactory, double explorationProb, double targetingProb,
                                double cumulativeStratExp, IBaseline.IFactory baselineFactory) {
        super(rmFactory, explorationProb, targetingProb, cumulativeStratExp);
        if (baselineFactory == null) {
            throw new IllegalArgumentException("Baseline factory can't be null!");
        }
        this.baselineFactory = baselineFactory;
    }

    @Override
    public BaseCFRSolver create(BaseCFRSolver.IStrategyAccumulationFilter accumulationFilter) {
        return new MCCFRSolver(rmFactory, accumulationFilter, explorationProb, targetingProb, cumulativeStratExp, baselineFactory);
    }

    @Override
    public String toString() {
        return "VR-MCCFR{" +
                "t=" + targetingProb +
                ",e=" + explorationProb +
                ",rm=" + rmFactory.getConfigString() +
                ",cse=" + cumulativeStratExp +
                ",bl=" + baselineFactory.getConfigString() +
                '}';
    }

    @Override
    public String getConfigString() {
        return toString();
    }
}
