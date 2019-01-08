package com.ggp.players;

import com.ggp.*;
import com.ggp.players.deepstack.trackers.IGameTraversalTracker;
import com.ggp.players.deepstack.trackers.SimpleTracker;
import com.ggp.solvers.cfr.BaseCFRSolver;
import com.ggp.utils.random.RandomSampler;
import com.ggp.utils.strategy.NormalizingInfoSetStrategyWrapper;
import com.ggp.utils.time.StopWatch;

import java.util.List;

public class SolvingPlayer implements IPlayer {
    public static class Factory implements IPlayerFactory {
        private BaseCFRSolver.Factory cfrSolverFactory;

        public Factory(BaseCFRSolver.Factory cfrSolverFactory) {
            this.cfrSolverFactory = cfrSolverFactory;
        }

        @Override
        public IPlayer create(IGameDescription game, int role) {
            return new SolvingPlayer(cfrSolverFactory.create(null), game, role);
        }

        @Override
        public String toString() {
            return "SolvingPlayer{" +
                        cfrSolverFactory +
                    '}';
        }

        @Override
        public String getConfigString() {
            return toString();
        }
    }

    private BaseCFRSolver cfrSolver;
    private IInformationSet currentInfoSet;
    private IGameTraversalTracker rootTracker;
    private final int role;
    private final RandomSampler sampler = new RandomSampler();

    public SolvingPlayer(BaseCFRSolver cfrSolver, IGameDescription gameDesc, int role) {
        this.cfrSolver = cfrSolver;
        this.currentInfoSet = gameDesc.getInitialInformationSet(role);
        this.rootTracker = SimpleTracker.createRoot(gameDesc.getInitialState());
        this.role = role;
    }

    @Override
    public void init(long timeoutMillis) {
        computeStrategy(timeoutMillis);
    }

    private void computeStrategy(long timeoutMillis) {
        StopWatch timer = new StopWatch();
        while (timer.getLiveDurationMs() < timeoutMillis) {
            cfrSolver.runIteration(rootTracker);
        }
    }

    private void doAction(IAction a) {
        currentInfoSet = currentInfoSet.next(a);
    }

    @Override
    public IAction act(long timeoutMillis) {
        computeStrategy(timeoutMillis);
        List<IAction> legalActions = currentInfoSet.getLegalActions();
        IInfoSetStrategy isStrat = new NormalizingInfoSetStrategyWrapper(cfrSolver.getCumulativeStrat().getInfoSetStrategy(currentInfoSet));
        int actionIdx = sampler.selectIdx(legalActions.size(), idx -> isStrat.getProbability(idx)).getResult();
        IAction a = legalActions.get(actionIdx);
        doAction(a);
        return a;
    }

    @Override
    public void forceAction(IAction a, long timeoutMillis) {
        computeStrategy(timeoutMillis);
        doAction(a);
    }

    @Override
    public int getRole() {
        return role;
    }

    @Override
    public void receivePercepts(IPercept percept) {
        currentInfoSet = currentInfoSet.applyPercept(percept);
    }
}
