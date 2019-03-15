package com.ggp.solvers.cfr;

import com.ggp.*;
import com.ggp.players.continual_resolving.cfrd.CFRDSubgameRoot;
import com.ggp.players.continual_resolving.cfrd.actions.FollowAction;
import com.ggp.players.continual_resolving.cfrd.actions.TerminateAction;
import com.ggp.players.continual_resolving.trackers.IGameTraversalTracker;
import com.ggp.solvers.cfr.baselines.NoBaseline;
import com.ggp.solvers.cfr.is_info.BaseCFRISInfo;
import com.ggp.solvers.cfr.is_info.MCCFRISInfo;
import com.ggp.solvers.cfr.utils.RandomNodeIS;
import com.ggp.utils.PlayerHelpers;
import com.ggp.utils.random.RandomSampler;

import java.util.List;
import java.util.function.Function;

public class MCCFRSolver extends BaseCFRSolver implements ITargetableSolver {
    public static class Factory extends BaseCFRSolver.Factory {
        protected final double explorationProb;
        protected final double targetingProb;
        protected final double cumulativeStratExp;

        public Factory(IRegretMatching.IFactory rmFactory, double explorationProb, double targetingProb, double cumulativeStratExp) {
            super(rmFactory);
            this.explorationProb = explorationProb;
            this.targetingProb = targetingProb;
            this.cumulativeStratExp = cumulativeStratExp;
        }

        @Override
        public BaseCFRSolver create(IStrategyAccumulationFilter accumulationFilter) {
            return new MCCFRSolver(rmFactory, accumulationFilter, explorationProb, targetingProb, cumulativeStratExp);
        }


        @Override
        public String getConfigString() {
            return "MC-CFR{" +
                    "t=" + targetingProb +
                    ",e=" + explorationProb +
                    ",rm=" + rmFactory.getConfigString() +
                    ",cse=" + cumulativeStratExp +
                    '}';
        }
    }

    private final double explorationProb;
    private final double targetingProb;
    private RandomSampler sampler = new RandomSampler();
    private long iterationCounter = 0;
    private IBaseline.IFactory baselineFactory;
    private final double cumulativeStratExp;
    private ISearchTargeting rootTargeting;
    private boolean isTargetedIteration = false;

    public MCCFRSolver(IRegretMatching.IFactory rmFactory, IStrategyAccumulationFilter accumulationFilter,
                       double explorationProb, double targetingProb, double cumulativeStratExp) {
        this(rmFactory, accumulationFilter, explorationProb, targetingProb, cumulativeStratExp, new NoBaseline.Factory());
    }

    public MCCFRSolver(IRegretMatching.IFactory rmFactory, IStrategyAccumulationFilter accumulationFilter, double explorationProb,
                       double targetingProb, double cumulativeStratExp, IBaseline.IFactory baselineFactory) {
        super(rmFactory, accumulationFilter);
        this.explorationProb = explorationProb;
        this.targetingProb = targetingProb;
        this.cumulativeStratExp = cumulativeStratExp;
        this.baselineFactory = baselineFactory;
    }

    protected MCCFRSolver(MCCFRSolver solver, IStrategyAccumulationFilter accumulationFilter) {
        super(solver, accumulationFilter);
        this.explorationProb = solver.explorationProb;
        this.targetingProb = solver.targetingProb;
        this.iterationCounter = solver.iterationCounter;
        this.baselineFactory = solver.baselineFactory;
        this.cumulativeStratExp = solver.cumulativeStratExp;
        this.rootTargeting = solver.rootTargeting;
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

    @Override
    public void setTargeting(ISearchTargeting targeting) {
        rootTargeting = targeting;
    }

    private SampleResult sample(ICompleteInformationState s, ISearchTargeting targeting, Function<Integer, Double> probMap, int size) {
        Function<Integer, Double> targetedProbMap = probMap;
        if (targeting != null) {
            List<Integer> targetedActions = targeting.target(s);
            if (targetedActions != null && !targetedActions.isEmpty()) {
                Function<Integer, Double> normalizedTargetedProbs = sampler.normalize(targetedActions, probMap);
                if (isTargetedIteration) {
                    RandomSampler.SampleResult<Integer> res = sampler.select(targetedActions, normalizedTargetedProbs);
                    return new SampleResult(res.getResult(), res.getSampleProb(), probMap.apply(res.getResult()));
                } else {
                    targetedProbMap = a -> targetedActions.contains(a) ? normalizedTargetedProbs.apply(a) : 0d;
                }
            }
        }
        RandomSampler.SampleResult<Integer> res = sampler.selectIdx(size, probMap);
        return new SampleResult(res.getResult(), targetedProbMap.apply(res.getResult()), res.getSampleProb());
    }

    private SampleResult sampleRandom(ICompleteInformationState s, ISearchTargeting targeting) {
        List<IAction> legalActions = s.getLegalActions();
        IRandomNode rndNode = s.getRandomNode();
        Function<Integer, Double> probMap  = actionIdx -> rndNode.getActionProb(legalActions.get(actionIdx));
        return sample(s, targeting, probMap, legalActions.size());
    }

    private SampleResult samplePlayerAction(ICompleteInformationState s, double[] strat, int player, ISearchTargeting targeting) {
        double unifPart = explorationProb * 1d/strat.length;
        Function<Integer, Double> probMap;
        if (s.getActingPlayerId() == player) {
            probMap = actionIdx -> unifPart + (1-explorationProb) * strat[actionIdx];
        } else {
            probMap = actionIdx -> strat[actionIdx];
        }
        return sample(s, targeting, probMap, strat.length);
    }

    private CFRResult playout(ICompleteInformationState s, double prefixProb, int player) {
        double suffixProb = 1;
        while (!s.isTerminal()) {
            visitedStates++;
            SampleResult res;
            if (s.isRandomNode()) {
                res = sampleRandom(s, null);
            } else {
                List<IAction> legalActions = s.getLegalActions();
                res = new SampleResult(sampler.selectIdx(legalActions),
                        1d/legalActions.size(), 1d/legalActions.size());
            }

            suffixProb *= res.untargetedProb;
            s = s.next(s.getLegalActions().get(res.actionIdx));
        }
        return new CFRResult(suffixProb, prefixProb * suffixProb, s.getPayoff(player)/suffixProb);
    }

    private CFRResult cfr(IGameTraversalTracker tracker, double playerProb, double opponentProb,
                          double targetedSampleProb, double untargetedSampleProb, int player, int depth, ISearchTargeting targeting) {
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
            SampleResult sample = sampleRandom(s, targeting);
            final double sampleProb = targetingProb * sample.targetedProb + (1-targetingProb) * sample.untargetedProb;
            IAction action = legalActions.get(sample.actionIdx);
            CFRResult res = cfr(tracker.next(action), playerProb, opponentProb,
                    sample.targetedProb * targetedSampleProb, sample.untargetedProb * untargetedSampleProb,
                    player, depth+1, (targeting != null) ? targeting.next(action, sample.actionIdx) : null);
            res.suffixReachProb *= sample.untargetedProb; // action prob == untargeted sampling prob for random node
            double utility = 0;
            int actionIdx = 0;
            for (IRandomNode.IRandomNodeAction rna: s.getRandomNode()) {
                IAction a = rna.getAction();
                double actionProb = rna.getProb();
                double baselineValue = baseline.getValue(actionIdx);
                if (actionIdx == sample.actionIdx) {
                    double actionUtil = (baselineValue + (res.utility - baselineValue)/sampleProb);
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
        SampleResult sampledAction = samplePlayerAction(s, isInfo.getStrat(), player, targeting);
        final double actionSampleProb = targetingProb * sampledAction.targetedProb + (1-targetingProb) * sampledAction.untargetedProb;
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
            IAction action = legalActions.get(sampledAction.actionIdx);
            ret = cfr(tracker.next(action), newPlayerProb, newOpponentProb,
                    sampledAction.targetedProb * targetedSampleProb,
                    sampledAction.untargetedProb * untargetedSampleProb, player,
                    depth+1, (targeting != null) ? targeting.next(action, sampledAction.actionIdx) : null);
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
                actionUtil = (baselineValue + (ret.utility - baselineValue) / actionSampleProb);
            }
            utility += prob * actionUtil;
            actionIdx++;
        }

        double probWithoutPlayer = opponentProb * tracker.getRndProb();
        double newSuffixReachProb = actionProb * ret.suffixReachProb;
        double cfv = probWithoutPlayer * utility / totalSampleProb;

        final double p1Utility = PlayerHelpers.selectByPlayerId(player, 1, -1) * utility / totalSampleProb;
        listeners.forEach(listener -> listener.leavingState(tracker, info, p1Utility));

        if (actingPlayer == player) {
            actionIdx = 0;
            for (IAction a: legalActions) {
                double baselineValue = baseline.getValue(actionIdx);
                double actionUtil;
                if (actionIdx == sampledAction.actionIdx) {
                    actionUtil = baselineValue + (ret.utility - baselineValue)/actionSampleProb;
                } else {
                    actionUtil = baselineValue;
                }
                double actionCFV = probWithoutPlayer * actionUtil / totalSampleProb;
                addRegret(isInfo, actionIdx, actionCFV - cfv);
                actionIdx++;
            }
        } else {
            if (accumulationFilter.isAccumulated(actingPlayerInfoSet)) {
                double[] strat = isInfo.getStrat();
                double[] cumulativeStrat = isInfo.getCumulativeStrat();
                double mul = Math.pow(((double) isInfo.getLastVisitedAtIteration()) / iterationCounter, cumulativeStratExp);
                for (int a = 0; a < legalActions.size(); ++a) {
                    // player != actingPlayer therefore probWithoutPlayer == acting player's prob
                    cumulativeStrat[a] = mul * cumulativeStrat[a] + probWithoutPlayer*strat[a]/totalSampleProb;
                }
            }
        }
        baseline.update(sampledAction.actionIdx, ret.utility);
        isInfo.setLastVisitedAtIteration(iterationCounter);
        ret.suffixReachProb = newSuffixReachProb;
        ret.utility = utility;
        return ret;
    }

    private double handleCFRDStart(IGameTraversalTracker tracker, double targetedSampleProb, double untargetedSampleProb, int player, ISearchTargeting targeting) {
        ICompleteInformationState s = tracker.getCurrentState();
        Info info = new Info(1,1, tracker.getRndProb());
        visitedStates++;
        listeners.forEach(listener -> listener.enteringState(tracker, info));
        double util = 0;
        if (s.getClass().equals(CFRDSubgameRoot.class)) {
            List<IAction> legalActions = s.getLegalActions();
            SampleResult sample = sampleRandom(s, targeting);
            IAction a = legalActions.get(sample.actionIdx);
            util = handleCFRDStart(tracker.next(a),
                    sample.targetedProb * targetedSampleProb,
                    sample.untargetedProb * untargetedSampleProb, player,
                    targeting != null ? targeting.next(a, sample.actionIdx) : null);
        } else {
            IInformationSet actingPlayerInfoSet = s.getInfoSetForActingPlayer();
            MCCFRISInfo isInfo = (MCCFRISInfo) getIsInfo(actingPlayerInfoSet);
            final int actingPlayer = s.getActingPlayerId();
            final double totalSampleProb = targetingProb * targetedSampleProb + (1-targetingProb) * untargetedSampleProb;
            double actionUtil[] = new double[2];
            // since both actions are always "sampled" we leave sampling probs as they are
            CFRResult followRes = cfr(tracker.next(FollowAction.instance), (actingPlayer == player) ? isInfo.getStrat()[0] : 1,
                    (actingPlayer != player) ? isInfo.getStrat()[0] : 1,
                    targetedSampleProb, untargetedSampleProb, player, 2, targeting != null ? targeting.next(FollowAction.instance, 0) : null);
            actionUtil[0] = followRes.utility / totalSampleProb;
            actionUtil[1] = s.next(TerminateAction.instance).getPayoff(player) / totalSampleProb;

            util = (isInfo.getStrat()[0] * actionUtil[0] + isInfo.getStrat()[1] * actionUtil[1]);

            if (actingPlayer == player) {
                for (int a = 0; a < 2; a++) {
                    addRegret(isInfo, a, tracker.getRndProb() * (actionUtil[a] - util));
                }
                isInfo.doRegretMatching();
            }
        }

        final double p1Utility = PlayerHelpers.selectByPlayerId(player, 1, -1) * util;
        listeners.forEach(listener -> listener.leavingState(tracker, info, p1Utility));
        return util;
    }

    @Override
    public void runIteration(IGameTraversalTracker tracker) {
        iterationCounter++;
        isTargetedIteration = sampler.choose(targetingProb);
        int player = (int)(iterationCounter % 2) + 1;
        if (tracker.getCurrentState().getClass().equals(CFRDSubgameRoot.class)) {
            handleCFRDStart(tracker, 1, 1, player, rootTargeting);
        } else {
            cfr(tracker, 1, 1, 1, 1, player, 0, rootTargeting);
        }
    }

    @Override
    protected BaseCFRISInfo initIsInfo(IInformationSet is) {
        int actionSize = is.getLegalActions().size();
        return new MCCFRISInfo(rmFactory, actionSize, baselineFactory);
    }

    @Override
    public BaseCFRSolver copy(IStrategyAccumulationFilter accumulationFilter) {
        return new MCCFRSolver(this, accumulationFilter);
    }

    @Override
    public boolean wantsTargeting() {
        return targetingProb > 0;
    }
}
