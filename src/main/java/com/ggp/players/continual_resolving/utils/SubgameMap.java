package com.ggp.players.continual_resolving.utils;

import com.ggp.ICompleteInformationState;
import com.ggp.IInformationSet;
import com.ggp.players.continual_resolving.cfrd.AugmentedIS.CFRDAugmentedCISWrapper;
import com.ggp.utils.PlayerHelpers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class SubgameMap {
    private class SubgameSet {
        public HashSet<IInformationSet> subgameInfosets = new HashSet<>();
        public HashSet<ICompleteInformationState> states = new HashSet<>();

        public void add(ICompleteInformationState s) {
            states.add(s);
        }

        public void addIS(IInformationSet is) {
            subgameInfosets.add(is);
        }

        public int size() {
            return states.size();
        }

        public void merge(SubgameSet other) {
            subgameInfosets.addAll(other.subgameInfosets);
            states.addAll(other.states);
        }
    }
    private final int opponentId;
    private HashMap<IInformationSet, SubgameSet> isToSubgame = new HashMap<>();

    public SubgameMap(int opponentId) {
        this.opponentId = opponentId;
    }

    private void replaceSubgame(SubgameSet oldSubgame, SubgameSet newSubgame) {
        for (IInformationSet is: oldSubgame.subgameInfosets) {
            isToSubgame.put(is, newSubgame);
        }
    }

    private void mergeSubgames(SubgameSet sub1, SubgameSet sub2, ICompleteInformationState newState) {
        if (sub1.size() > sub2.size()) {
            sub1.add(newState);
            sub1.merge(sub2);
            replaceSubgame(sub2, sub1);
        } else {
            sub2.add(newState);
            sub2.merge(sub1);
            replaceSubgame(sub1, sub2);
        }
    }

    public void addSubgameState(ICompleteInformationState s, ICompleteInformationState subgameRoot) {
        IInformationSet is1, is2;
        if (s.getActingPlayerId() != PlayerHelpers.getOpponentId(opponentId)) {
            throw new RuntimeException("Only states where my player acts can be added to subgame!");
        }
        if (opponentId == 1) {
            is1 = ((CFRDAugmentedCISWrapper)s).getOpponentsAugmentedIS();
            is2 = s.getInfoSetForPlayer(2);
        } else {
            is1 = s.getInfoSetForPlayer(1);
            is2 = ((CFRDAugmentedCISWrapper)s).getOpponentsAugmentedIS();
        }
        SubgameSet sub1 = isToSubgame.getOrDefault(is1, null),
                sub2 = isToSubgame.getOrDefault(is2, null);
        SubgameSet merged = null;
        if (sub1 != null && sub2 != null) {
            if (sub1 == sub2) {
                sub1.add(subgameRoot);
            } else {
                mergeSubgames(sub1, sub2, subgameRoot);
            }
            return;
        } else if (sub1 != null) {
            sub1.add(subgameRoot);
            sub1.addIS(is2);
            merged = sub1;
        } else if (sub2 != null) {
            sub2.add(subgameRoot);
            sub2.addIS(is1);
            merged = sub2;
        } else {
            merged = new SubgameSet();
            merged.add(subgameRoot);
            merged.addIS(is1);
            merged.addIS(is2);
        }
        isToSubgame.put(is1, merged);
        isToSubgame.put(is2, merged);
    }

    public Set<ICompleteInformationState> getSubgame(IInformationSet is) {
        SubgameSet s =  isToSubgame.getOrDefault(is, null);
        if (s != null) return s.states;
        return null;
    }
}
