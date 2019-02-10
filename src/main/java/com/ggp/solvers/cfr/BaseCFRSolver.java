package com.ggp.solvers.cfr;

import com.ggp.IInfoSetStrategy;
import com.ggp.IInformationSet;
import com.ggp.IStrategy;
import com.ggp.players.deepstack.trackers.IGameTraversalTracker;
import com.ggp.solvers.cfr.is_info.BaseCFRISInfo;
import com.ggp.utils.strategy.InfoSetStrategy;
import com.ggp.utils.strategy.Strategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class BaseCFRSolver {
    public static abstract class Factory {
        protected IRegretMatching.IFactory rmFactory;

        public Factory(IRegretMatching.IFactory rmFactory) {
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

    protected IRegretMatching.IFactory rmFactory;
    protected HashMap<IInformationSet, BaseCFRISInfo> isInfos = new HashMap<>();
    protected IStrategyAccumulationFilter accumulationFilter;
    protected List<DepthLimitedCFRSolver.IListener> listeners = new ArrayList<>();
    protected long visitedStates = 0;
    private double totalRegret = 0;

    public BaseCFRSolver(IRegretMatching.IFactory rmFactory, IStrategyAccumulationFilter accumulationFilter) {
        this.rmFactory = rmFactory;
        if (accumulationFilter == null) accumulationFilter = getDefaultStrategyAccumulationFilter();
        this.accumulationFilter = accumulationFilter;
    }

    /**
     * Run 1 iteration of CFR
     * @param tracker
     */
    public abstract void runIteration(IGameTraversalTracker tracker);

    protected boolean isInMemory(IInformationSet is) {
        return isInfos.containsKey(is);
    }

    protected BaseCFRISInfo initIsInfo(IInformationSet is) {
        return new BaseCFRISInfo(rmFactory, is.getLegalActions().size());
    }

    protected BaseCFRISInfo getIsInfo(IInformationSet is) {
        return isInfos.computeIfAbsent(is, k -> initIsInfo(is));
    }

    protected void addRegret(BaseCFRISInfo isInfo, int actionIdx, double regretDiff) {
        IRegretMatching rm = isInfo.getRegretMatching();
        totalRegret -= Math.max(0, rm.getRegret(actionIdx));
        rm.addActionRegret(actionIdx, regretDiff);
        totalRegret += Math.max(0, rm.getRegret(actionIdx));
    }

    public void registerListener(DepthLimitedCFRSolver.IListener listener) {
        if (listener != null) listeners.add(listener);
    }

    /**
     * Get runtime cumulative strategy
     *
     * Use this to obtain current cumulative strategy during solving.
     * @return
     */
    public IStrategy getCumulativeStrat() {
        return new IStrategy() {
            @Override
            public Iterable<IInformationSet> getDefinedInformationSets() {
                return accumulationFilter.getAccumulated();
            }

            @Override
            public boolean isDefined(IInformationSet is) {
                return accumulationFilter.isAccumulated(is);
            }

            @Override
            public IInfoSetStrategy getInfoSetStrategy(IInformationSet is) {
                int actionSize = is.getLegalActions().size();
                BaseCFRISInfo info = isInfos.getOrDefault(is, null);
                double[] probs = (info == null ? null : info.getCumulativeStrat());
                return new IInfoSetStrategy() {
                    @Override
                    public double getProbability(int actionIdx) {
                        if (actionIdx < 0 || actionIdx >= actionSize) return 0;
                        if (probs == null) return 1d/actionSize;
                        return probs[actionIdx];
                    }

                    @Override
                    public int size() {
                        return actionSize;
                    }
                };
            }
        };
    }

    /**
     * Get final cumulative strategy
     *
     * Use this to obtain cumulative strategy after solving is done. Doesn't reference IS infos.
     * @return
     */
    public Strategy getFinalCumulativeStrat() {
        Strategy ret = new Strategy();
        accumulationFilter.getAccumulated().forEach(is ->
                ret.setInfoSetStrategy(is, InfoSetStrategy.fromArrayReference(getIsInfo(is).getCumulativeStrat())));
        return ret;
    }

    public IStrategyAccumulationFilter getDefaultStrategyAccumulationFilter() {
        return new IStrategyAccumulationFilter() {
            @Override
            public boolean isAccumulated(IInformationSet is) {
                return isInfos.containsKey(is);
            }

            @Override
            public Iterable<IInformationSet> getAccumulated() {
                return isInfos.keySet();
            }
        };
    }

    public long getVisitedStates() {
        return visitedStates;
    }

    public double getTotalRegret() {
        return totalRegret;
    }
}
