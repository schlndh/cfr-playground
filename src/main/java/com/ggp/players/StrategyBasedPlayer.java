package com.ggp.players;

import com.ggp.*;
import com.ggp.utils.PlayerHelpers;
import com.ggp.utils.random.RandomSampler;
import com.ggp.utils.strategy.NormalizingStrategyWrapper;
import com.ggp.utils.strategy.Strategy;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;

public class StrategyBasedPlayer implements IPlayer {
    public static class Factory implements IPlayerFactory {
        private static final long serialVersionUID = 1L;
        private IStrategy strategy;

        public Factory(IStrategy strategy) {
            this.strategy = strategy;
        }

        @Override
        public IPlayer create(IGameDescription game, int role) {
            return new StrategyBasedPlayer(role, game, strategy);
        }

        @Override
        public String getConfigString() {
            return "StrategyBasedPlayer{}";
        }
    }

    public static class DynamiclyLoadedStrategyFactory implements IPlayerFactory {
        private static final long serialVersionUID = 1L;
        private HashMap<IGameDescription, IStrategy> strategies = new HashMap<>();
        private String strategyDir;

        public DynamiclyLoadedStrategyFactory(String strategyDir) {
            if (strategyDir == null || "".equals(strategyDir)) {
                throw new IllegalArgumentException("Strategy directory name must be non-empty!");
            }
            this.strategyDir = strategyDir;
        }

        @Override
        public IPlayer create(IGameDescription game, int role) {
            IStrategy gameStrat = strategies.computeIfAbsent(game, k -> {
                try (FileInputStream fileInputStream = new FileInputStream(strategyDir + "/" + game.getConfigString() + ".strat")) {
                    ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                    IStrategy strat = (IStrategy) objectInputStream.readObject();
                    if (strat == null) return null;
                    return new NormalizingStrategyWrapper(strat);
                } catch (Exception e) {
                    return null;
                }
            });
            if (gameStrat == null) {
                throw new RuntimeException("Failed to load strategy for " + game.getConfigString());
            }
            return new StrategyBasedPlayer(role, game, gameStrat);
        }

        @Override
        public String getConfigString() {
            return "StrategyBasedPlayer{\"" + strategyDir + "\"}";
        }
    }

    private int myId;
    private IInformationSet hiddenInfo;
    private IStrategy strategy;
    private RandomSampler sampler = new RandomSampler();

    public StrategyBasedPlayer(int myId, IGameDescription gameDesc, IStrategy strategy) {
        this.myId = myId;
        this.hiddenInfo = gameDesc.getInitialInformationSet(myId);
        this.strategy = strategy;
    }

    @Override
    public void init(long timeoutMillis) {
    }

    @Override
    public IAction act(long timeoutMillis) {
        IAction a = PlayerHelpers.sampleAction(sampler, hiddenInfo, strategy);
        hiddenInfo = hiddenInfo.next(a);
        return a;
    }

    @Override
    public void forceAction(IAction a, long timeoutMillis) {
        hiddenInfo = hiddenInfo.next(a);
    }

    @Override
    public int getRole() {
        return myId;
    }

    @Override
    public void receivePercepts(IPercept percept) {
        hiddenInfo = hiddenInfo.applyPercept(percept);
    }
}
