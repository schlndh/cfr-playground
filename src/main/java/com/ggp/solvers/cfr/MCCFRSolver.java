package com.ggp.solvers.cfr;

import com.ggp.*;
import com.ggp.players.deepstack.IRegretMatching;
import com.ggp.players.deepstack.trackers.IGameTraversalTracker;
import com.ggp.solvers.cfr.baselines.NoBaseline;
import com.ggp.IInfoSetStrategy;
import com.ggp.utils.PlayerHelpers;
import com.ggp.utils.random.RandomSampler;

import java.util.List;
import java.util.Objects;

public class MCCFRSolver extends BaseCFRSolver {
    public static class Factory extends BaseCFRSolver.Factory {
        protected final double explorationProb;
        protected final double targetingProb;

        public Factory(IRegretMatching.Factory rmFactory) {
            this(rmFactory, 0.2, 0);
        }

        public Factory(IRegretMatching.Factory rmFactory, double explorationProb, double targetingProb) {
            super(rmFactory);
            this.explorationProb = explorationProb;
            this.targetingProb = targetingProb;
        }

        @Override
        public BaseCFRSolver create(IStrategyAccumulationFilter accumulationFilter) {
            return new MCCFRSolver(rmFactory.create(), accumulationFilter, explorationProb, targetingProb);
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
    private int iterationCounter = 0;
    private IBaseline baseline1, baseline2;

    public MCCFRSolver(IRegretMatching regretMatching, IStrategyAccumulationFilter accumulationFilter, double explorationProb, double targetingProb) {
        this(regretMatching, accumulationFilter, explorationProb, targetingProb, new NoBaseline.Factory());
    }

    public MCCFRSolver(IRegretMatching regretMatching, IStrategyAccumulationFilter accumulationFilter, double explorationProb,
                       double targetingProb, IBaseline.IFactory baselineFactory) {
        super(regretMatching, accumulationFilter);
        this.explorationProb = explorationProb;
        this.targetingProb = targetingProb;
        this.baseline1 = baselineFactory.create();
        this.baseline2 = baselineFactory.create();
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
        RandomSampler.SampleResult<Integer> res = sampler.selectIdx(legalActions, a -> rndNode.getActionProb(legalActions.get(a)));
        return new SampleResult(res.getResult(), res.getSampleProb(), res.getSampleProb());
    }

    private SampleResult samplePlayerAction(ICompleteInformationState s, IInformationSet is, int player) {
        List<IAction> legalActions = is.getLegalActions();
        double unifPart = explorationProb * 1d/legalActions.size();
        int actingPlayer = s.getActingPlayerId();
        RandomSampler.SampleResult<Integer> res;
        IInfoSetStrategy isStrat = strat.getInfoSetStrategy(is);
        if (actingPlayer == player) {
            res = sampler.selectIdx(legalActions, actionIdx -> unifPart + (1-explorationProb) * isStrat.getProbability(actionIdx));
        } else {
            res = sampler.selectIdx(legalActions, actionIdx -> isStrat.getProbability(actionIdx));
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

    private static class RandomActionWrapper implements IAction {
        private IAction action;

        public RandomActionWrapper(IAction action) {
            this.action = action;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RandomActionWrapper that = (RandomActionWrapper) o;
            return Objects.equals(action, that.action);
        }

        @Override
        public int hashCode() {
            return Objects.hash(action);
        }
    }

    private CFRResult cfr(IGameTraversalTracker tracker, double playerProb, double opponentProb,
                          double targetedSampleProb, double untargetedSampleProb, int player) {
        ICompleteInformationState s = tracker.getCurrentState();
        Info info = PlayerHelpers.callWithOrderedParams(player, playerProb, opponentProb, (prob1, prob2) -> new Info(prob1, prob2, tracker.getRndProb()));
        visitedStates++;
        listeners.forEach(listener -> listener.enteringState(tracker, info));
        IBaseline baseline = PlayerHelpers.selectByPlayerId(player, baseline1, baseline2);

        double totalSampleProb = targetingProb * targetedSampleProb + (1-targetingProb) * untargetedSampleProb;
        if (s.isTerminal()) {
            return new CFRResult(1,
                    totalSampleProb,
                    s.getPayoff(player));
        }
        List<IAction> legalActions = s.getLegalActions();
        if (legalActions == null || legalActions.isEmpty()) return null;
        if (s.isRandomNode()) {
            SampleResult sample = sampleRandom(s);
            CFRResult res = cfr(tracker.next(legalActions.get(sample.actionIdx)), playerProb, opponentProb,
                    sample.targetedProb * targetedSampleProb, sample.untargetedProb * untargetedSampleProb, player);
            res.suffixReachProb *= sample.untargetedProb;
            double utility = 0;
            int actionIdx = 0;
            for (IRandomNode.IRandomNodeAction rna: s.getRandomNode()) {
                IAction a = rna.getAction();
                double actionProb = rna.getProb();
                // wrapper is used to distinguish random actions from regular actions in case IS is the same before and after the random action
                IAction wrappedAction = new RandomActionWrapper(a);
                double baselineValue = baseline.getValue(s.getInfoSetForPlayer(player), wrappedAction);
                if (actionIdx == sample.actionIdx) {
                    double actionUtil = (baselineValue + (res.utility - baselineValue)/sample.untargetedProb);
                    utility += actionProb * actionUtil;
                    baseline.update(s.getInfoSetForPlayer(player), wrappedAction, res.utility);
                } else {
                    utility += actionProb * baselineValue;
                }
                actionIdx++;
            }

            res.utility = utility;
            return res;
        }

        IInformationSet actingPlayerInfoSet = s.getInfoSetForActingPlayer();
        boolean isInMemory = regretMatching.hasInfoSet(actingPlayerInfoSet);
        regretMatching.getRegretMatchedStrategy(actingPlayerInfoSet, strat);
        SampleResult sampledAction = samplePlayerAction(s, actingPlayerInfoSet, player);
        CFRResult ret;
        IInfoSetStrategy isStrat = strat.getInfoSetStrategy(actingPlayerInfoSet);
        double actionProb = isStrat.getProbability(sampledAction.actionIdx);
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
                    sampledAction.untargetedProb * untargetedSampleProb, player);
        } else {
            ret = playout(s.next(legalActions.get(sampledAction.actionIdx)), (totalSampleProb)/legalActions.size(), player);
        }
        double utility = 0;
        int actionIdx = 0;
        for (IAction a: legalActions) {
            double prob = isStrat.getProbability(actionIdx);
            double baselineValue = baseline.getValue(actingPlayerInfoSet, a);
            double actionUtil = baselineValue;
            if (actionIdx == sampledAction.actionIdx) {
                actionUtil = (baselineValue + (ret.utility - baselineValue) / sampledAction.untargetedProb);
                baseline.update(actingPlayerInfoSet, a, ret.utility);
            }
            utility += prob * actionUtil;
            actionIdx++;
        }

        double probWithoutPlayer = opponentProb * tracker.getRndProb();
        double newSuffixReachProb = actionProb * ret.suffixReachProb;
        int actingPlayer = s.getActingPlayerId();
        double cfv = probWithoutPlayer * utility / totalSampleProb;

        final double p1Utility = PlayerHelpers.selectByPlayerId(player, 1, -1) * utility;
        listeners.forEach(listener -> listener.leavingState(tracker, info, p1Utility));

        if (actingPlayer == player) {
            actionIdx = 0;
            for (IAction a: legalActions) {
                double baselineValue = baseline.getValue(actingPlayerInfoSet, a);
                double actionUtil;
                if (actionIdx == sampledAction.actionIdx) {
                    actionUtil = baselineValue + (ret.utility - baselineValue)/sampledAction.untargetedProb;
                } else {
                    actionUtil = baselineValue;
                }
                double actionCFV = probWithoutPlayer * actionUtil / totalSampleProb;
                regretMatching.addActionRegret(actingPlayerInfoSet, actionIdx, actionCFV - cfv);
                actionIdx++;
            }
        } else {
            if (accumulationFilter.isAccumulated(actingPlayerInfoSet)) {
                cumulativeStrat.addProbabilities(actingPlayerInfoSet, a ->
                        probWithoutPlayer*isStrat.getProbability(a)/totalSampleProb);
            }
        }
        ret.suffixReachProb = newSuffixReachProb;
        ret.utility = utility;
        return ret;
    }

    @Override
    public void runIteration(IGameTraversalTracker tracker) {
        iterationCounter++;
        cfr(tracker, 1, 1, 1, 1, (iterationCounter % 2) + 1);
    }
}
