package com.ggp.solvers.cfr;

import com.ggp.players.deepstack.IRegretMatching;

public class VRMCCFRSolverFactory extends MCCFRSolver.Factory {
    private IBaseline.IFactory baselineFactory;

    public VRMCCFRSolverFactory(IRegretMatching.Factory rmFactory, IBaseline.IFactory baselineFactory) {
        super(rmFactory);
        this.baselineFactory = baselineFactory;
    }

    public VRMCCFRSolverFactory(IRegretMatching.Factory rmFactory, double explorationProb, double targetingProb, IBaseline.IFactory baselineFactory) {
        super(rmFactory, explorationProb, targetingProb);
        this.baselineFactory = baselineFactory;
    }

    @Override
    public BaseCFRSolver create(BaseCFRSolver.IStrategyAccumulationFilter accumulationFilter) {
        return new MCCFRSolver(rmFactory.create(), accumulationFilter, explorationProb, targetingProb, baselineFactory);
    }

    @Override
    public String toString() {
        return "VR-MCCFR{" +
                "t=" + targetingProb +
                ",e=" + explorationProb +
                ",rm=" + rmFactory.getConfigString() +
                ",bl=" + baselineFactory.getConfigString() +
                '}';
    }

    @Override
    public String getConfigString() {
        return toString();
    }
}
