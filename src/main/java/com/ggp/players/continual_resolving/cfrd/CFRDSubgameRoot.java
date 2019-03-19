package com.ggp.players.continual_resolving.cfrd;

import com.ggp.*;
import com.ggp.players.continual_resolving.cfrd.AugmentedIS.CFRDAugmentedCISWrapper;
import com.ggp.players.continual_resolving.cfrd.AugmentedIS.CFRDAugmentedIS;
import com.ggp.players.continual_resolving.cfrd.actions.SelectCISAction;
import com.ggp.players.continual_resolving.cfrd.percepts.ISSelectedPercept;
import com.ggp.players.continual_resolving.utils.CISRange;
import com.ggp.utils.PlayerHelpers;

import java.util.*;

public class CFRDSubgameRoot implements ICompleteInformationState {
    private static final long serialVersionUID = 1L;
    private CISRange range;
    private Map<IInformationSet, Double> opponentCFV;
    private int opponentId;
    private final List<IAction> legalActions;
    private final HashMap<IInformationSet, Double> opponentIsReachProbs;

    public CFRDSubgameRoot(CISRange range, Map<IInformationSet, Double> opponentCFV, int opponentId) {
        this.range = range;
        this.opponentCFV = opponentCFV;
        this.opponentId = opponentId;

        this.opponentIsReachProbs = new HashMap<>();
        ArrayList<IAction> legalActions = new ArrayList<>(range.size());
        for (Map.Entry<ICompleteInformationState, Double> entry: range.getProbabilities()) {
            ICompleteInformationState s = entry.getKey();
            legalActions.add(new SelectCISAction(s, entry.getValue()/range.getNorm()));
            this.opponentIsReachProbs.merge(((CFRDAugmentedCISWrapper)s).getOpponentsAugmentedIS(), entry.getValue(), (oldV, newV) -> oldV + newV);
        }
        this.legalActions = Collections.unmodifiableList(legalActions);
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public int getActingPlayerId() {
        return 0;
    }

    @Override
    public double getPayoff(int player) {
        return 0;
    }

    @Override
    public List<IAction> getLegalActions() {
        return legalActions;
    }

    @Override
    public IInformationSet getInfoSetForPlayer(int player) {
        return new CFRDRootIS(player, this);
    }

    @Override
    public ICompleteInformationState next(IAction a) {
        if (!isLegal(a)) return null;
        SelectCISAction sel = (SelectCISAction) a;
        ICompleteInformationState s = sel.getSelectedState();
        CFRDAugmentedIS is = ((CFRDAugmentedCISWrapper)s).getOpponentsAugmentedIS();
        double isReachProb = opponentIsReachProbs.get(is);
        return new OpponentsChoiceState(s, opponentId, opponentCFV.get(is)/isReachProb);
    }

    @Override
    public Iterable<IPercept> getPercepts(IAction a) {
        if (!isLegal(a)) return null;
        SelectCISAction sel = (SelectCISAction) a;
        ICompleteInformationState s = sel.getSelectedState();
        int myId = PlayerHelpers.getOpponentId(opponentId);
        return Arrays.asList(new ISSelectedPercept(opponentId, ((CFRDAugmentedCISWrapper)s).getOpponentsAugmentedIS()),
                new ISSelectedPercept(myId, s.getInfoSetForPlayer(myId)));
    }

    @Override
    public IRandomNode getRandomNode() {
        return new IRandomNode() {
            @Override
            public double getActionProb(IAction a) {
                if (a == null || a.getClass() != SelectCISAction.class) return 0;
                SelectCISAction sel = (SelectCISAction) a;
                return sel.getProb();
            }

            @Override
            public Iterator<IRandomNodeAction> iterator() {
                return new Iterator<IRandomNodeAction>() {
                    private int idx = 0;
                    @Override
                    public boolean hasNext() {
                        return idx < legalActions.size();
                    }

                    @Override
                    public IRandomNodeAction next() {
                        return (SelectCISAction) legalActions.get(idx++);
                    }
                };
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CFRDSubgameRoot that = (CFRDSubgameRoot) o;
        return opponentId == that.opponentId &&
                Objects.equals(range, that.range) &&
                Objects.equals(opponentCFV, that.opponentCFV);
    }

    @Override
    public int hashCode() {
        return Objects.hash(range, opponentCFV, opponentId);
    }

    int getOpponentId() {
        return opponentId;
    }
}
