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
                                long opponentCfvNorm, ArrayList<IEvaluablePlayer.IListener> resolvingListeners);
        String getConfigString();
    }

    class ActResult {
        public IStrategy cumulativeStrategy;
        public SubgameMap subgameMap;
        public HashMap<ICompleteInformationState, Double> nextRange;
        public HashMap<IInformationSet, Double> nextOpponentCFV;
        public long norm;

        public ActResult(IStrategy cumulativeStrategy, SubgameMap subgameMap, HashMap<ICompleteInformationState, Double> nextRange,
                         HashMap<IInformationSet, Double> nextOpponentCFV, long norm) {
            this.cumulativeStrategy = cumulativeStrategy;
            this.subgameMap = subgameMap;
            this.nextRange = nextRange;
            this.nextOpponentCFV = nextOpponentCFV;
            this.norm = norm;
        }
    }

    ActResult act(IterationTimer timeout);
    void init(ICompleteInformationState initialState, IterationTimer timeout);
    ISubgameResolver copy(ArrayList<IEvaluablePlayer.IListener> resolvingListeners);
}
