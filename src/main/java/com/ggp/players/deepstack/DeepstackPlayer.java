package com.ggp.players.deepstack;

import com.ggp.*;
import com.ggp.players.deepstack.utils.*;
import com.ggp.utils.PlayerHelpers;
import com.ggp.utils.random.RandomSampler;

import java.util.*;

public class DeepstackPlayer implements IPlayer {
    public static class Factory implements IPlayerFactory {
        private static final long serialVersionUID = 1L;
        private ISubgameResolver.Factory resolverFactory;
        private IResolvingListener listener;

        public Factory(ISubgameResolver.Factory resolverFactory, IResolvingListener listener) {
            this.resolverFactory = resolverFactory;
            this.listener = listener;
        }

        @Override
        public DeepstackPlayer create(IGameDescription game, int role) {
            DeepstackPlayer ret = new DeepstackPlayer(role, game, resolverFactory);
            if (listener != null) ret.registerResolvingListener(listener);
            return ret;
        }
    }

    private int id;
    private int opponentId;
    private CISRange range;
    private IInformationSet hiddenInfo;
    private HashMap<IInformationSet, Double> opponentCFV;
    private IGameDescription gameDesc;
    private IAction myLastAction;
    private IStrategy lastCumulativeStrategy;
    private ArrayList<IResolvingListener> resolvingListeners = new ArrayList<>();
    private ISubgameResolver.Factory resolverFactory;
    private SubgameMap subgameMap;
    private NextRangeTree nrt;
    private RandomSampler sampler = new RandomSampler();

    private DeepstackPlayer(int id, CISRange range, IInformationSet hiddenInfo,
                            IGameDescription gameDesc,
                            IAction myLastAction,
                            IStrategy lastCumulativeStrategy, ArrayList<IResolvingListener> resolvingListeners,
                            ISubgameResolver.Factory resolverFactory, SubgameMap subgameMap, NextRangeTree nrt, HashMap<IInformationSet, Double> opponentCFV) {
        this.id = id;
        this.opponentId = PlayerHelpers.getOpponentId(id);
        this.range = range;
        this.hiddenInfo = hiddenInfo;
        this.gameDesc = gameDesc;
        this.myLastAction = myLastAction;
        this.lastCumulativeStrategy = lastCumulativeStrategy;
        this.resolvingListeners = resolvingListeners;
        this.resolverFactory = resolverFactory;
        this.subgameMap = subgameMap;
        this.nrt = nrt;
        this.opponentCFV = opponentCFV;
    }

    public DeepstackPlayer(int id, IGameDescription gameDesc, ISubgameResolver.Factory resolverFactory) {
        this.id = id;
        this.opponentId = PlayerHelpers.getOpponentId(id);
        IInformationSet initialSet = gameDesc.getInitialInformationSet(id);
        hiddenInfo = initialSet;
        IInformationSet initialOpponentSet = gameDesc.getInitialInformationSet(opponentId);
        range = new CISRange(gameDesc.getInitialState());
        opponentCFV = new HashMap<>(1);
        opponentCFV.put(initialOpponentSet, 0d);
        this.gameDesc = gameDesc;
        this.resolverFactory = resolverFactory;
    }

    public void registerResolvingListener(IResolvingListener listener) {
        if (listener != null) resolvingListeners.add(listener);
    }

    public void unregisterResolvingListener(IResolvingListener listener) {
        if (listener != null) resolvingListeners.remove(listener);
    }

    private ISubgameResolver createResolver() {
        return resolverFactory.create(id, hiddenInfo, range, opponentCFV, resolvingListeners);
    }

    @Override
    public void init(long timeoutMillis) {
        IterationTimer timer = new IterationTimer(timeoutMillis);
        timer.start();
        ISubgameResolver r = createResolver();
        ISubgameResolver.InitResult res = r.init(gameDesc.getInitialState(), timer);
        this.subgameMap = res.subgameMap;
        this.opponentCFV = res.nextOpponentCFV;
        this.nrt = res.nrt;
    }

    @Override
    public int getRole() {
        return id;
    }

    public ISubgameResolver.ActResult computeStrategy(long timeoutMillis) {
        IterationTimer timer = new IterationTimer(timeoutMillis);
        timer.start();
        range = new CISRange(subgameMap.getSubgame(hiddenInfo), nrt, lastCumulativeStrategy);
        ISubgameResolver r = createResolver();
        return r.act(timer);
    }

    public DeepstackPlayer copy() {
        return new DeepstackPlayer(id, range, hiddenInfo,
                gameDesc, myLastAction,
                lastCumulativeStrategy, resolvingListeners, resolverFactory, subgameMap, nrt, opponentCFV);
    }

    private IAction act(IAction forcedAction, long timeoutMillis) {
        ISubgameResolver.ActResult res = computeStrategy(timeoutMillis);
        IAction selectedAction;
        if (forcedAction == null) {
            selectedAction = PlayerHelpers.sampleAction(sampler, hiddenInfo, res.cumulativeStrategy);
        } else {
            selectedAction = forcedAction;
        }

        act(selectedAction, res);
        return selectedAction;
    }

    public void act(IAction selectedAction, ISubgameResolver.ActResult res) {
        lastCumulativeStrategy = res.cumulativeStrategy;
        myLastAction = selectedAction;
        subgameMap = res.subgameMap;
        nrt = res.nrt;
        opponentCFV = res.nextOpponentCFV;
        hiddenInfo = hiddenInfo.next(selectedAction);
    }

    @Override
    public void forceAction(IAction forcedAction, long timeoutMillis) {
        act(forcedAction, timeoutMillis);
    }

    @Override
    public IAction act(long timeoutMillis) {
        return act(null, timeoutMillis);
    }

    @Override
    public void receivePercepts(IPercept percept) {
        hiddenInfo = hiddenInfo.applyPercept(percept);
    }

    public String getConfigString() {
        return "DeepstackPlayer{" +
                "subgameResolver=" + resolverFactory.getConfigString() +
                '}';
    }
}
