package com.ggp.solvers.cfr;

import com.ggp.IInformationSet;
import com.ggp.players.deepstack.IRegretMatching;
import com.ggp.players.deepstack.trackers.IGameTraversalTracker;
import com.ggp.players.deepstack.utils.Strategy;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseCFRSolver {
    public static abstract class Factory {
        protected IRegretMatching.Factory rmFactory;

        public Factory(IRegretMatching.Factory rmFactory) {
            this.rmFactory = rmFactory;
        }

        public abstract BaseCFRSolver create(IStrategyAccumulationFilter accumulationFilter);
        public abstract String getConfigString();
    }

    public static class Info {
        public final double reachProb1, reachProb2, rndProb;

        public Info(double reachProb1, double reachProb2, double rndProb) {
            this.reachProb1 = reachProb1;
            this.reachProb2 = reachProb2;
            this.rndProb = rndProb;
        }
    }

    public interface IListener {
        void enteringState(IGameTraversalTracker tracker, DepthLimitedCFRSolver.Info info);
        void leavingState(IGameTraversalTracker tracker, DepthLimitedCFRSolver.Info info, double p1Utility);
    }

    public interface IStrategyAccumulationFilter {
        boolean isAccumulated(IInformationSet is);
        Iterable<IInformationSet> getAccumulated();
    }

    protected IRegretMatching regretMatching;
    protected Strategy strat = new Strategy();
    protected Strategy cumulativeStrat = new Strategy();
    protected IStrategyAccumulationFilter accumulationFilter;
    protected List<DepthLimitedCFRSolver.IListener> listeners = new ArrayList<>();
    protected long visitedStates = 0;

    public BaseCFRSolver(IRegretMatching regretMatching, IStrategyAccumulationFilter accumulationFilter) {
        this.regretMatching = regretMatching;
        if (accumulationFilter == null) accumulationFilter = getDefaultStrategyAccumulationFilter();
        this.accumulationFilter = accumulationFilter;
    }

    /**
     * Run 1 iteration of CFR
     * @param tracker
     */
    public abstract void runIteration(IGameTraversalTracker tracker);

    public void registerListener(DepthLimitedCFRSolver.IListener listener) {
        if (listener != null) listeners.add(listener);
    }

    public Strategy getCumulativeStrat() {
        return cumulativeStrat;
    }

    public IStrategyAccumulationFilter getDefaultStrategyAccumulationFilter() {
        return new IStrategyAccumulationFilter() {
            @Override
            public boolean isAccumulated(IInformationSet is) {
                return strat.hasInformationSet(is);
            }

            @Override
            public Iterable<IInformationSet> getAccumulated() {
                return strat.getDefinedInformationSets();
            }
        };
    }

    public long getVisitedStates() {
        return visitedStates;
    }

    public double getTotalRegret() {
        return regretMatching.getTotalRegret();
    }
}
