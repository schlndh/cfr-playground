package com.ggp.player_evaluators;

import com.ggp.*;
import com.ggp.player_evaluators.listeners.StrategyAggregatorListener;
import com.ggp.utils.CompleteInformationStateWrapper;
import com.ggp.IInfoSetStrategy;
import com.ggp.utils.PlayerHelpers;
import com.ggp.utils.recall.PerfectRecallGameDescriptionWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Evaluates Deepstack configuration by traversing the game tree and computing strategy at each decision point,
 * while aggregating the resulting strategies at given time intervals.
 */
public class TraversingEvaluator implements IPlayerEvaluator {
    public static class Factory implements IFactory {
        @Override
        public IPlayerEvaluator create(int initMs, List<Integer> logPointsMs) {
            return new TraversingEvaluator(initMs, logPointsMs);
        }

        @Override
        public String getConfigString() {
            return "TraversingEvaluator{}";
        }
    }

    private int initMs;
    private int timeoutMs;
    private ArrayList<Integer> logPointsMs;
    private boolean quiet;

    /**
     * Constructor
     * @param initMs timeout for deepstack initialization
     * @param logPointsMs ASC ordered list of times when strategies should be aggregated
     */
    public TraversingEvaluator(int initMs, List<Integer> logPointsMs) {
        this.initMs = initMs;
        this.timeoutMs = logPointsMs.get(logPointsMs.size() - 1);
        this.logPointsMs = new ArrayList<>(logPointsMs);
    }

    private void applyPercepts(IEvaluablePlayer pl1, IEvaluablePlayer pl2, Iterable<IPercept> percepts) {
        for (IPercept p: percepts) {
            PlayerHelpers.selectByPlayerId(p.getTargetPlayer(), pl1, pl2).receivePercepts(p);
        }
    }

    private String getActionStr(int actionIdx, int actions) {
        return " " + (actionIdx + 1) + "/" + actions;
    }

    private void printAction(int actionIdx, int actions) {
        if (quiet) return;
        System.out.print(getActionStr(actionIdx, actions));
    }

    private void unprintAction(int actionIdx, int actions) {
        if (quiet) return;
        String tmp = "";
        String actionStr = getActionStr(actionIdx, actions);
        String clearStr = "";
        for (int i = 0; i < actionStr.length(); ++i) {
            tmp += "\b";
            clearStr += " ";
        }
        System.out.print(tmp + clearStr + tmp);
    }

    private static class ActCacheEntry {
        public IEvaluablePlayer playerWithComputedStrat;
        public List<EvaluatorEntry> entries;

        public ActCacheEntry(IEvaluablePlayer playerWithComputedStrat, List<EvaluatorEntry> entries) {
            this.playerWithComputedStrat = playerWithComputedStrat;
            this.entries = entries;
        }
    }

    private void aggregateStrategy(HashMap<IInformationSet, ActCacheEntry> actCache, List<EvaluatorEntry> entries, CompleteInformationStateWrapper sw, IEvaluablePlayer pl1, IEvaluablePlayer pl2, double reachProb1, double reachProb2, int depth, long[] pathStates) {
        ICompleteInformationState s = sw.getOrigState();
        if (s.isTerminal()) {
            for (int i = 0; i < entries.size(); ++i) {
                entries.get(i).addPathStates(pathStates[2*i], reachProb1);
                entries.get(i).addPathStates(pathStates[2*i + 1], reachProb2);
            }
            return;
        }

        List<IAction> legalActions = s.getLegalActions();
        if (s.isRandomNode()) {
            IRandomNode rndNode = s.getRandomNode();
            int actionIdx = 0;
            for (IRandomNode.IRandomNodeAction rndAction: rndNode) {
                IAction a = rndAction.getAction();
                double actionProb = rndAction.getProb();
                IEvaluablePlayer npl1 = pl1.copy(), npl2 = pl2.copy();
                applyPercepts(npl1, npl2, s.getPercepts(a));
                printAction(actionIdx, legalActions.size());
                aggregateStrategy(actCache, entries, (CompleteInformationStateWrapper) sw.next(a), npl1, npl2, reachProb1 * actionProb, reachProb2 * actionProb, depth + 1, pathStates);
                unprintAction(actionIdx, legalActions.size());
                actionIdx++;
            }
            return;
        }
        double playerReachProb = PlayerHelpers.selectByPlayerId(s.getActingPlayerId(), reachProb1, reachProb2);
        IInformationSet is = s.getInfoSetForActingPlayer();

        ActCacheEntry cacheEntry = actCache.computeIfAbsent(sw.getInfoSetForActingPlayer(), k -> {
            IEvaluablePlayer currentPlayer = PlayerHelpers.selectByPlayerId(s.getActingPlayerId(), pl1, pl2);
            StrategyAggregatorListener strategyAggregatorListener = new StrategyAggregatorListener(logPointsMs);
            strategyAggregatorListener.initEnd(null);
            currentPlayer.registerResolvingListener(strategyAggregatorListener);
            currentPlayer.computeStrategy(timeoutMs);
            currentPlayer.unregisterResolvingListener(strategyAggregatorListener);
            return new ActCacheEntry(currentPlayer, strategyAggregatorListener.getEntries());
        });

        long newPathStates[] = Arrays.copyOf(pathStates, pathStates.length);
        for (int i = 0; i < entries.size(); ++i) {
            EvaluatorEntry cachedEntry = cacheEntry.entries.get(i);
            IInfoSetStrategy cachedStrat = cachedEntry.getAggregatedStrat().getInfoSetStrategy(is);
            entries.get(i).addTime(cachedEntry.getEntryTimeMs(), playerReachProb);
            entries.get(i).getAggregatedStrat().addProbabilities(is, actionIdx -> playerReachProb * cachedStrat.getProbability(actionIdx));
            entries.get(i).addVisitedStates(cachedEntry.getAvgVisitedStates());
            newPathStates[2*i + s.getActingPlayerId() - 1] += cachedEntry.getAvgVisitedStates();
        }

        int actionIdx = 0;
        IInfoSetStrategy isStrat = cacheEntry.playerWithComputedStrat.getNormalizedSubgameStrategy().getInfoSetStrategy(is);
        for (IAction a: legalActions) {
            double actionProb = isStrat.getProbability(actionIdx);
            double nrp1 = reachProb1, nrp2 = reachProb2;
            IEvaluablePlayer npl1 = null, npl2 = null;
            if (s.getActingPlayerId() == 1) {
                npl1 = cacheEntry.playerWithComputedStrat.copy();
                npl2 = pl2.copy();
                npl1.actWithPrecomputedStrategy(a);
                nrp1 *= actionProb;
            } else if (s.getActingPlayerId() == 2) {
                npl1 = pl1.copy();
                npl2 = cacheEntry.playerWithComputedStrat.copy();
                npl2.actWithPrecomputedStrategy(a);
                nrp2 *= actionProb;
            }
            applyPercepts(npl1, npl2, s.getPercepts(a));
            printAction(actionIdx, legalActions.size());
            aggregateStrategy(actCache, entries, (CompleteInformationStateWrapper) sw.next(a), npl1, npl2, nrp1, nrp2, depth + 1, newPathStates);
            unprintAction(actionIdx, legalActions.size());
            actionIdx++;
        }
    }

    @Override
    public List<EvaluatorEntry> evaluate(IGameDescription gameDesc, IEvaluablePlayer.IFactory playerFactory, boolean quiet) {
        this.quiet = quiet;
        ICompleteInformationState initialState = gameDesc.getInitialState();
        IEvaluablePlayer pl1 = playerFactory.create(gameDesc, 1), pl2 = playerFactory.create(gameDesc, 2);
        pl1.init(initMs);
        pl2.init(initMs);
        List<EvaluatorEntry> entries = new ArrayList<>(logPointsMs.size());
        for (int i = 0; i < logPointsMs.size(); ++i) {
            entries.add(new EvaluatorEntry(logPointsMs.get(i)));
        }
        long pathStates[] = new long[entries.size()*2];
        HashMap<IInformationSet, ActCacheEntry> actCache = new HashMap<>();
        aggregateStrategy(actCache, entries, (CompleteInformationStateWrapper) PerfectRecallGameDescriptionWrapper.wrapInitialState(initialState), pl1.copy(), pl2.copy(), 1d, 1d, 0, pathStates);

        for (EvaluatorEntry entry: entries) {
            entry.getAggregatedStrat().normalize();
            entry.setVisitedStatesNorm(1);
        }
        return entries;
    }
}
