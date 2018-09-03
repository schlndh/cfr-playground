package com.ggp.players.deepstack.resolvers;

import com.ggp.IAction;
import com.ggp.ICompleteInformationState;
import com.ggp.IInformationSet;
import com.ggp.players.deepstack.IRegretMatching;
import com.ggp.players.deepstack.IResolvingInfo;
import com.ggp.players.deepstack.IResolvingListener;
import com.ggp.players.deepstack.ISubgameResolver;
import com.ggp.players.deepstack.rerget_matching.RegretMatchingPlus;
import com.ggp.players.deepstack.utils.*;
import com.ggp.utils.PlayerHelpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseCFRSolver implements ISubgameResolver {
    protected final int myId;
    protected InformationSetRange range;
    protected HashMap<IInformationSet, Double> opponentCFV;
    protected List<IResolvingListener> resolvingListeners;
    protected IResolvingInfo resInfo;
    protected Strategy cumulativeStrat = new Strategy();
    protected IRegretMatching regretMatching = new RegretMatchingPlus();
    protected SubgameGadget subgameGadget;
    protected final int opponentId;

    public BaseCFRSolver(int myId, InformationSetRange range, HashMap<IInformationSet, Double> opponentCFV,
                         List<IResolvingListener> resolvingListeners) {
        this.myId = myId;
        this.range = range;
        this.opponentCFV = opponentCFV;
        this.resolvingListeners = resolvingListeners;
        if (this.resolvingListeners == null) this.resolvingListeners = new ArrayList<>();
        this.subgameGadget = new SubgameGadget(opponentCFV);
        this.opponentId = PlayerHelpers.getOpponentId(myId);
    }

    protected void findMyNextTurn(GameTreeTraversalTracker tracker) {
        ICompleteInformationState s = tracker.getCurrentState();
        if (s.isTerminal()) return;
        if (tracker.isMyNextTurnReached()) {
            tracker.getNtit().addLeaf(s.getInfoSetForPlayer(opponentId), 0);
            tracker.getPsMap().add(tracker.getMyPerceptSequence(), tracker.getOpponentPerceptSequence());
            tracker.getNrt().add(tracker.getOpponentPerceptSequence(), s, tracker.getMyTopAction(), tracker.getRndProb());
            return;
        }
        for (IAction a: s.getLegalActions()) {
            findMyNextTurn(tracker.next(a));
        }
    }

    protected GameTreeTraversalTracker prepareDataStructures() {
        GameTreeTraversalTracker tracker = GameTreeTraversalTracker.createForAct(myId);
        for (Map.Entry<ICompleteInformationState, Double> stateProb: range.getProbabilities()) {
            GameTreeTraversalTracker stateTracker = tracker.visitRandom(stateProb.getKey(), stateProb.getValue());
            findMyNextTurn(stateTracker);
        }
        return tracker;
    }

    protected abstract ActResult doAct(GameTreeTraversalTracker tracker, IterationTimer timeout);

    @Override
    public ActResult act(IterationTimer timeout) {
        resolvingListeners.forEach(listener -> listener.resolvingStart(resInfo));
        GameTreeTraversalTracker tracker = prepareDataStructures();
        ActResult res = doAct(tracker, timeout);
        resolvingListeners.forEach(listener -> listener.resolvingEnd(resInfo));
        return res;
    }

    protected abstract InitResult doInit(GameTreeTraversalTracker tracker, IterationTimer timeout);

    @Override
    public InitResult init(ICompleteInformationState initialState, IterationTimer timeout) {
        resolvingListeners.forEach(listener -> listener.resolvingStart(resInfo));
        GameTreeTraversalTracker tracker = GameTreeTraversalTracker.createForInit(myId, initialState);
        findMyNextTurn(tracker);
        InitResult res = doInit(tracker, timeout);
        resolvingListeners.forEach(listener -> listener.resolvingEnd(resInfo));
        resolvingListeners.forEach(listener -> listener.initEnd(resInfo));
        return res;
    }
}