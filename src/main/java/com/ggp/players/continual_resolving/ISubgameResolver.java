package com.ggp.players.continual_resolving;

import com.ggp.*;
import com.ggp.player_evaluators.IEvaluablePlayer;
import com.ggp.players.continual_resolving.utils.*;
import com.ggp.utils.time.IterationTimer;

import java.util.ArrayList;
import java.util.HashMap;

public interface ISubgameResolver {
    interface Factory {
        ISubgameResolver create(int myId, IInformationSet hiddenInfo, CISRange myRange, HashMap<IInformationSet, Double> opponentCFV,
                                ArrayList<IEvaluablePlayer.IListener> resolvingListeners);
        String getConfigString();
    }

    class ActResult {
        public IStrategy cumulativeStrategy;
        public SubgameMap subgameMap;
        public NextRangeTree nrt;
        public HashMap<IInformationSet, Double> nextOpponentCFV;

        public ActResult(IStrategy cumulativeStrategy, SubgameMap subgameMap, NextRangeTree nrt, HashMap<IInformationSet, Double> nextOpponentCFV) {
            this.cumulativeStrategy = cumulativeStrategy;
            this.subgameMap = subgameMap;
            this.nrt = nrt;
            this.nextOpponentCFV = nextOpponentCFV;
        }
    }

    ActResult act(IterationTimer timeout);
    void init(ICompleteInformationState initialState, IterationTimer timeout);
    ISubgameResolver copy(ArrayList<IEvaluablePlayer.IListener> resolvingListeners);
}
