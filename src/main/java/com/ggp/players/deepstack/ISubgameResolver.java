package com.ggp.players.deepstack;

import com.ggp.*;
import com.ggp.players.deepstack.utils.*;

import java.util.ArrayList;
import java.util.HashMap;

public interface ISubgameResolver {
    interface Factory {
        ISubgameResolver create(int myId, IInformationSet hiddenInfo, CISRange myRange, HashMap<IInformationSet, Double> opponentCFV,
                                ArrayList<IResolvingListener> resolvingListeners);
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

    class InitResult {
        public SubgameMap subgameMap;
        public NextRangeTree nrt;
        public HashMap<IInformationSet, Double> nextOpponentCFV;

        public InitResult(SubgameMap subgameMap, NextRangeTree nrt, HashMap<IInformationSet, Double> nextOpponentCFV) {
            this.subgameMap = subgameMap;
            this.nrt = nrt;
            this.nextOpponentCFV = nextOpponentCFV;
        }
    }

    ActResult act(IterationTimer timeout);
    InitResult init(ICompleteInformationState initialState, IterationTimer timeout);
}
