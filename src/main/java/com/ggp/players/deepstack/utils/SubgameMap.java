package com.ggp.players.deepstack.utils;

import com.ggp.ICompleteInformationState;
import com.ggp.IInformationSet;
import com.ggp.players.deepstack.cfrd.AugmentedIS.CFRDAugmentedCISWrapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class SubgameMap {
    private final int opponentId;
    private HashMap<IInformationSet, HashSet<ICompleteInformationState>> isToSubgame = new HashMap<>();

    public SubgameMap(int opponentId) {
        this.opponentId = opponentId;
    }

    public void addSubgameState(ICompleteInformationState s) {
        IInformationSet is1, is2;
        if (opponentId == 1) {
            is1 = ((CFRDAugmentedCISWrapper)s).getOpponentsAugmentedIS();
            is2 = s.getInfoSetForPlayer(2);
        } else {
            is1 = s.getInfoSetForPlayer(1);
            is2 = ((CFRDAugmentedCISWrapper)s).getOpponentsAugmentedIS();
        }
        HashSet<ICompleteInformationState> sub1 = isToSubgame.getOrDefault(is1, null),
                sub2 = isToSubgame.getOrDefault(is2, null);
        HashSet<ICompleteInformationState> merged = null;
        if (sub1 != null && sub2 != null) {
            if (sub1.size() > sub2.size()) {
                sub1.add(s);
                sub1.addAll(sub2);
                merged = sub1;
            } else {
                sub2.add(s);
                sub2.addAll(sub1);
                merged = sub2;
            }
        } else if (sub1 != null) {
            sub1.add(s);
            merged = sub1;
        } else if (sub2 != null) {
            sub2.add(s);
            merged = sub2;
        } else {
            merged = new HashSet<>();
            merged.add(s);
        }
        isToSubgame.put(is1, merged);
        isToSubgame.put(is2, merged);
    }

    public Set<ICompleteInformationState> getSubgame(IInformationSet is) {
        return isToSubgame.getOrDefault(is, null);
    }
}
