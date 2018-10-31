package com.ggp.solvers.cfr;

import com.ggp.*;
import com.ggp.players.deepstack.IRegretMatching;
import com.ggp.players.deepstack.trackers.IGameTraversalTracker;
import com.ggp.utils.PlayerHelpers;
import com.ggp.utils.random.RandomSampler;

import java.util.List;

public class MCCFRSolver extends BaseCFRSolver {
    public static class Factory extends BaseCFRSolver.Factory {
        private final double explorationProb = 0.2;
        private final double targetingProb = 0;

        public Factory(IRegretMatching.Factory rmFactory) {
            super(rmFactory);
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
    }
    private final double explorationProb;
    private final double targetingProb;
    private RandomSampler sampler = new RandomSampler();
    private int iterationCounter = 0;

    public MCCFRSolver(IRegretMatching regretMatching, IStrategyAccumulationFilter accumulationFilter, double explorationProb, double targetingProb) {
        super(regretMatching, accumulationFilter);
        this.explorationProb = explorationProb;
        this.targetingProb = targetingProb;
    }

    private static class CFRResult {
        public double suffixReachProb;
        public double sampleProb;
        public double payoff;

        public CFRResult(double suffixReachProb, double sampleProb, double payoff) {
            this.suffixReachProb = suffixReachProb;
            this.sampleProb = sampleProb;
            this.payoff = payoff;
        }
    }

    private static class SampleResult {
        public IAction action;
        public double targetedProb;
        public double untargetedProb;

        public SampleResult(IAction action, double targetedProb, double untargetedProb) {
            this.action = action;
            this.targetedProb = targetedProb;
            this.untargetedProb = untargetedProb;
        }
    }

    private SampleResult sampleRandom(ICompleteInformationState s) {
        List<IAction> legalActions = s.getLegalActions();
        IRandomNode rndNode = s.getRandomNode();
        RandomSampler.SampleResult<IAction> res = sampler.select(legalActions, a -> rndNode.getActionProb(a));
        return new SampleResult(res.getResult(), res.getSampleProb(), res.getSampleProb());
    }

    private SampleResult samplePlayerAction(ICompleteInformationState s, IInformationSet is, int player) {
        List<IAction> legalActions = is.getLegalActions();
        double unifPart = explorationProb * 1d/legalActions.size();
        int actingPlayer = s.getActingPlayerId();
        RandomSampler.SampleResult<IAction> res;
        if (actingPlayer == player) {
            res = sampler.select(legalActions, a -> unifPart + (1-explorationProb) * strat.getProbability(is, a));
        } else {
            res = sampler.select(legalActions, a -> strat.getProbability(is, a));
        }
        return new SampleResult(res.getResult(), res.getSampleProb(), res.getSampleProb());
    }

    private CFRResult playout(ICompleteInformationState s, double prefixProb, int player) {
        double suffixProb = 1;
        while (!s.isTerminal()) {
            SampleResult res;
            if (s.isRandomNode()) {
                res = sampleRandom(s);
            } else {
                List<IAction> legalActions = s.getLegalActions();
                res = new SampleResult(sampler.select(legalActions),
                        1d/legalActions.size(), 1d/legalActions.size());
            }

            suffixProb *= res.untargetedProb;
            s = s.next(res.action);
        }
        return new CFRResult(suffixProb, prefixProb * suffixProb, s.getPayoff(player));
    }

    private CFRResult cfr(IGameTraversalTracker tracker, double playerProb, double opponentProb,
                          double targetedSampleProb, double untargetedSampleProb, int player) {
        ICompleteInformationState s = tracker.getCurrentState();
        Info info = PlayerHelpers.callWithOrderedParams(player, playerProb, opponentProb, (prob1, prob2) -> new Info(prob1, prob2, tracker.getRndProb()));
        listeners.forEach(listener -> listener.enteringState(tracker, info));

        double totalSampleProb = targetingProb * targetedSampleProb + (1-targetingProb) * untargetedSampleProb;
        if (s.isTerminal()) {
            return new CFRResult(1,
                    totalSampleProb,
                    s.getPayoff(player));
        } else if (s.isRandomNode()) {
            SampleResult sample = sampleRandom(s);
            CFRResult res = cfr(tracker.next(sample.action), playerProb, opponentProb,
                    sample.targetedProb * targetedSampleProb, sample.untargetedProb * untargetedSampleProb, player);
            res.suffixReachProb *= sample.untargetedProb;
            return res;
        }

        IInformationSet actingPlayerInfoSet = s.getInfoSetForActingPlayer();
        List<IAction> legalActions = actingPlayerInfoSet.getLegalActions();
        if (legalActions == null || legalActions.isEmpty()) return null;
        SampleResult sampledAction = samplePlayerAction(s, actingPlayerInfoSet, player);
        CFRResult ret;
        double actionProb = strat.getProbability(actingPlayerInfoSet, sampledAction.action);
        if (regretMatching.hasInfoSet(actingPlayerInfoSet)) {
            regretMatching.getRegretMatchedStrategy(actingPlayerInfoSet, strat);
            double newPlayerProb = playerProb;
            double newOpponentProb = opponentProb;
            if (s.getActingPlayerId() == player) {
                newPlayerProb *= actionProb;
            } else {
                newOpponentProb *= actionProb;
            }
            ret = cfr(tracker.next(sampledAction.action), newPlayerProb, newOpponentProb,
                    sampledAction.targetedProb * targetedSampleProb,
                    sampledAction.untargetedProb * untargetedSampleProb, player);
        } else {
            regretMatching.initInfoSet(actingPlayerInfoSet);
            ret = playout(s, (totalSampleProb)/legalActions.size(), player);
        }

        double probWithoutPlayer = opponentProb * tracker.getRndProb();
        double newSuffixReachProb = actionProb * ret.suffixReachProb;
        int actingPlayer = s.getActingPlayerId();
        double w = ret.payoff * probWithoutPlayer / ret.sampleProb;
        double cfv = w * newSuffixReachProb;
        double sampledActionCfv = w * ret.suffixReachProb;

        final double p1Utility = PlayerHelpers.selectByPlayerId(player, 1, -1)
                * ret.payoff * newSuffixReachProb / ret.sampleProb;
        listeners.forEach(listener -> listener.leavingState(tracker, info, p1Utility));

        if (actingPlayer == player) {
            int actionIdx = 0;
            for (IAction a: legalActions) {
                double regretDiff;
                if (a.equals(sampledAction.action)) {
                    regretDiff = sampledActionCfv - cfv;
                } else {
                    regretDiff = -cfv;
                }
                regretMatching.addActionRegret(actingPlayerInfoSet, actionIdx, regretDiff);
                actionIdx++;
            }
        } else {
            if (accumulationFilter.isAccumulated(actingPlayerInfoSet)) {
                cumulativeStrat.addProbabilities(actingPlayerInfoSet, action ->
                        probWithoutPlayer*strat.getProbability(actingPlayerInfoSet, action)/totalSampleProb);
            }
        }
        ret.suffixReachProb = newSuffixReachProb;
        return ret;
    }

    @Override
    public void runIteration(IGameTraversalTracker tracker) {
        iterationCounter++;
        cfr(tracker, 1, 1, 1, 1, (iterationCounter % 2) + 1);
    }
}
