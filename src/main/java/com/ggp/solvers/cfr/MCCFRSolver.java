package com.ggp.solvers.cfr;

import com.ggp.*;
import com.ggp.players.deepstack.trackers.IGameTraversalTracker;
import com.ggp.solvers.cfr.baselines.NoBaseline;
import com.ggp.IInfoSetStrategy;
import com.ggp.solvers.cfr.is_info.BaseCFRISInfo;
import com.ggp.solvers.cfr.is_info.MCCFRISInfo;
import com.ggp.solvers.cfr.utils.RandomNodeIS;
import com.ggp.utils.PlayerHelpers;
import com.ggp.utils.random.RandomSampler;

import java.util.List;
import java.util.Objects;

public class MCCFRSolver extends BaseCFRSolver {
    public static class Factory extends BaseCFRSolver.Factory {
        protected final double explorationProb;
        protected final double targetingProb;

        public Factory(IRegretMatching.IFactory rmFactory, double explorationProb, double targetingProb) {
            super(rmFactory);
            this.explorationProb = explorationProb;
            this.targetingProb = targetingProb;
        }

        @Override
        public BaseCFRSolver create(IStrategyAccumulationFilter accumulationFilter) {
            return new MCCFRSolver(rmFactory, accumulationFilter, explorationProb, targetingProb);
        }


        @Override
        public String getConfigString() {
            return "MC-CFR{" +
                    "t=" + targetingProb +
                    ",e=" + explorationProb +
                    ",rm=" + rmFactory.getConfigString() +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Factory factory = (Factory) o;
            return Double.compare(factory.explorationProb, explorationProb) == 0 &&
                    Double.compare(factory.targetingProb, targetingProb) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(explorationProb, targetingProb);
        }
    }

    private final double explorationProb;
    private final double targetingProb;
    private RandomSampler sampler = new RandomSampler();
    private long iterationCounter = 0;
    private IBaseline.IFactory baselineFactory;

    public MCCFRSolver(IRegretMatching.IFactory rmFactory, IStrategyAccumulationFilter accumulationFilter, double explorationProb, double targetingProb) {
        this(rmFactory, accumulationFilter, explorationProb, targetingProb, new NoBaseline.Factory());
    }

    public MCCFRSolver(IRegretMatching.IFactory rmFactory, IStrategyAccumulationFilter accumulationFilter, double explorationProb,
                       double targetingProb, IBaseline.IFactory baselineFactory) {
        super(rmFactory, accumulationFilter);
        this.explorationProb = explorationProb;
        this.targetingProb = targetingProb;
        this.baselineFactory = baselineFactory;
    }

    private static class CFRResult {
        public double suffixReachProb;
        public double sampleProb;
        public double utility;

        public CFRResult(double suffixReachProb, double sampleProb, double utility) {
            this.suffixReachProb = suffixReachProb;
            this.sampleProb = sampleProb;
            this.utility = utility;
        }
    }

    private static class SampleResult {
        public int actionIdx;
        public double targetedProb;
        public double untargetedProb;

        public SampleResult(int actionIdx, double targetedProb, double untargetedProb) {
            this.actionIdx = actionIdx;
            this.targetedProb = targetedProb;
            this.untargetedProb = untargetedProb;
        }
    }

    private SampleResult sampleRandom(ICompleteInformationState s) {
        List<IAction> legalActions = s.getLegalActions();
        IRandomNode rndNode = s.getRandomNode();
        RandomSampler.SampleResult<Integer> res = sampler.selectIdx(legalActions.size(), a -> rndNode.getActionProb(legalActions.get(a)));
        return new SampleResult(res.getResult(), res.getSampleProb(), res.getSampleProb());
    }

    private SampleResult samplePlayerAction(int actingPlayer, double[] strat, int player) {
        double unifPart = explorationProb * 1d/strat.length;
        RandomSampler.SampleResult<Integer> res;
        if (actingPlayer == player) {
            res = sampler.selectIdx(strat.length, actionIdx -> unifPart + (1-explorationProb) * strat[actionIdx]);
        } else {
            res = sampler.selectIdx(strat.length, actionIdx -> strat[actionIdx]);
        }
        return new SampleResult(res.getResult(), res.getSampleProb(), res.getSampleProb());
    }

    private CFRResult playout(ICompleteInformationState s, double prefixProb, int player) {
        double suffixProb = 1;
        while (!s.isTerminal()) {
            visitedStates++;
            SampleResult res;
            if (s.isRandomNode()) {
                res = sampleRandom(s);
            } else {
                List<IAction> legalActions = s.getLegalActions();
                res = new SampleResult(sampler.selectIdx(legalActions),
                        1d/legalActions.size(), 1d/legalActions.size());
            }

            suffixProb *= res.untargetedProb;
            s = s.next(s.getLegalActions().get(res.actionIdx));
        }
        return new CFRResult(suffixProb, prefixProb * suffixProb, s.getPayoff(player));
    }

    private CFRResult cfr(IGameTraversalTracker tracker, double playerProb, double opponentProb,
                          double targetedSampleProb, double untargetedSampleProb, int player, int depth) {
        ICompleteInformationState s = tracker.getCurrentState();
        Info info = PlayerHelpers.callWithOrderedParams(player, playerProb, opponentProb, (prob1, prob2) -> new Info(prob1, prob2, tracker.getRndProb()));
        visitedStates++;
        listeners.forEach(listener -> listener.enteringState(tracker, info));


        double totalSampleProb = targetingProb * targetedSampleProb + (1-targetingProb) * untargetedSampleProb;
        if (s.isTerminal()) {
            return new CFRResult(1,
                    totalSampleProb,
                    s.getPayoff(player));
        }
        List<IAction> legalActions = s.getLegalActions();
        if (legalActions == null || legalActions.isEmpty()) return null;
        if (s.isRandomNode()) {
            MCCFRISInfo isInfo = (MCCFRISInfo) getIsInfo(new RandomNodeIS(depth, legalActions));
            IBaseline baseline = isInfo.getBaseline(player);
            SampleResult sample = sampleRandom(s);
            CFRResult res = cfr(tracker.next(legalActions.get(sample.actionIdx)), playerProb, opponentProb,
                    sample.targetedProb * targetedSampleProb, sample.untargetedProb * untargetedSampleProb, player, depth+1);
            res.suffixReachProb *= sample.untargetedProb;
            double utility = 0;
            int actionIdx = 0;
            for (IRandomNode.IRandomNodeAction rna: s.getRandomNode()) {
                IAction a = rna.getAction();
                double actionProb = rna.getProb();
                double baselineValue = baseline.getValue(actionIdx);
                if (actionIdx == sample.actionIdx) {
                    double actionUtil = (baselineValue + (res.utility - baselineValue)/sample.untargetedProb);
                    utility += actionProb * actionUtil;
                    baseline.update(actionIdx, res.utility);
                } else {
                    utility += actionProb * baselineValue;
                }
                actionIdx++;
            }

            res.utility = utility;
            return res;
        }
        IInformationSet actingPlayerInfoSet = s.getInfoSetForActingPlayer();
        boolean isInMemory = isInMemory(actingPlayerInfoSet);
        MCCFRISInfo isInfo = (MCCFRISInfo) getIsInfo(actingPlayerInfoSet);
        IBaseline baseline = isInfo.getBaseline(player);
        int actingPlayer = s.getActingPlayerId();

        isInfo.doRegretMatching();
        SampleResult sampledAction = samplePlayerAction(actingPlayer, isInfo.getStrat(), player);
        CFRResult ret;
        double actionProb = isInfo.getStrat()[sampledAction.actionIdx];
        if (isInMemory) {
            double newPlayerProb = playerProb;
            double newOpponentProb = opponentProb;
            if (s.getActingPlayerId() == player) {
                newPlayerProb *= actionProb;
            } else {
                newOpponentProb *= actionProb;
            }
            ret = cfr(tracker.next(legalActions.get(sampledAction.actionIdx)), newPlayerProb, newOpponentProb,
                    sampledAction.targetedProb * targetedSampleProb,
                    sampledAction.untargetedProb * untargetedSampleProb, player, depth+1);
        } else {
            ret = playout(s.next(legalActions.get(sampledAction.actionIdx)), (totalSampleProb)/legalActions.size(), player);
        }
        double utility = 0;
        int actionIdx = 0;
        for (IAction a: legalActions) {
            double prob = isInfo.getStrat()[actionIdx];
            double baselineValue = baseline.getValue(actionIdx);
            double actionUtil = baselineValue;
            if (actionIdx == sampledAction.actionIdx) {
                actionUtil = (baselineValue + (ret.utility - baselineValue) / sampledAction.untargetedProb);
                baseline.update(actionIdx, ret.utility);
            }
            utility += prob * actionUtil;
            actionIdx++;
        }

        double probWithoutPlayer = opponentProb * tracker.getRndProb();
        double newSuffixReachProb = actionProb * ret.suffixReachProb;
        double cfv = probWithoutPlayer * utility / totalSampleProb;

        final double p1Utility = PlayerHelpers.selectByPlayerId(player, 1, -1) * utility;
        listeners.forEach(listener -> listener.leavingState(tracker, info, p1Utility));

        if (actingPlayer == player) {
            actionIdx = 0;
            for (IAction a: legalActions) {
                double baselineValue = baseline.getValue(actionIdx);
                double actionUtil;
                if (actionIdx == sampledAction.actionIdx) {
                    actionUtil = baselineValue + (ret.utility - baselineValue)/sampledAction.untargetedProb;
                } else {
                    actionUtil = baselineValue;
                }
                double actionCFV = probWithoutPlayer * actionUtil / totalSampleProb;
                addRegret(isInfo, actionIdx, actionCFV - cfv);
                actionIdx++;
            }
        } else {
            if (accumulationFilter.isAccumulated(actingPlayerInfoSet)) {
                double[] cumulativeStrat = isInfo.getCumulativeStrat();
                for (int a = 0; a < legalActions.size(); ++a) {
                    cumulativeStrat[a] += probWithoutPlayer*isInfo.getStrat()[a]/totalSampleProb;
                }
            }
        }
        ret.suffixReachProb = newSuffixReachProb;
        ret.utility = utility;
        return ret;
    }

    @Override
    public void runIteration(IGameTraversalTracker tracker) {
        iterationCounter++;
        cfr(tracker, 1, 1, 1, 1, (int)(iterationCounter % 2) + 1, 0);
    }

    @Override
    protected BaseCFRISInfo initIsInfo(IInformationSet is) {
        int actionSize = is.getLegalActions().size();
        return new MCCFRISInfo(rmFactory, actionSize, baselineFactory);
    }
}
