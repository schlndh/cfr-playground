package com.ggp.players.deepstack.cfrd;

import com.ggp.*;
import com.ggp.players.deepstack.cfrd.actions.SelectCISAction;
import com.ggp.players.deepstack.cfrd.percepts.ISSelectedPercept;
import com.ggp.players.deepstack.utils.CISRange;

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
            legalActions.add(new SelectCISAction(entry.getKey(), entry.getValue()/range.getNorm()));
            this.opponentIsReachProbs.merge(entry.getKey().getInfoSetForPlayer(opponentId), entry.getValue(), (oldV, newV) -> oldV + newV);
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
        double isReachProb = opponentIsReachProbs.get(s.getInfoSetForPlayer(opponentId));
        return new OpponentsChoiceState(s, opponentId, opponentCFV.get(s.getInfoSetForPlayer(opponentId))/isReachProb);
    }

    @Override
    public Iterable<IPercept> getPercepts(IAction a) {
        if (!isLegal(a)) return null;
        SelectCISAction sel = (SelectCISAction) a;
        ICompleteInformationState s = sel.getSelectedState();
        return Arrays.asList(new ISSelectedPercept(1, s.getInfoSetForPlayer(1)),
                new ISSelectedPercept(2, s.getInfoSetForPlayer(2)));
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
