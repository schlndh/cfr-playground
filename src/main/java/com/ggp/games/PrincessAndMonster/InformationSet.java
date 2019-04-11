package com.ggp.games.PrincessAndMonster;

import com.ggp.IAction;
import com.ggp.IInformationSet;
import com.ggp.IPercept;

import java.util.List;
import java.util.Objects;

public class InformationSet implements IInformationSet {
    private static final long serialVersionUID = 1L;
    private final GameDescription gameDesc;
    private final int owner;
    private final IGraphPosition myPosition;
    private final int myTurns;

    public InformationSet(GameDescription gameDesc, int owner, IGraphPosition myPosition, int myTurns) {
        this.gameDesc = gameDesc;
        this.owner = owner;
        this.myPosition = myPosition;
        this.myTurns = myTurns;
    }

    @Override
    public IInformationSet next(IAction a) {
        return new InformationSet(gameDesc, owner, (IGraphPosition) a, myTurns + 1);
    }

    @Override
    public IInformationSet applyPercept(IPercept p) {
        return null;
    }

    @Override
    public List<IAction> getLegalActions() {
        return gameDesc.getGraph().getLegalMoves(myPosition);
    }

    @Override
    public boolean isLegal(IAction a) {
        return gameDesc.getGraph().isLegal(myPosition, a);
    }

    @Override
    public boolean isValid(IPercept p) {
        return false;
    }

    @Override
    public int getOwnerId() {
        return owner;
    }

    public GameDescription getGameDesc() {
        return gameDesc;
    }

    public IGraphPosition getMyPosition() {
        return myPosition;
    }

    public int getMyTurns() {
        return myTurns;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InformationSet that = (InformationSet) o;
        return owner == that.owner &&
                myTurns == that.myTurns &&
                Objects.equals(gameDesc, that.gameDesc) &&
                Objects.equals(myPosition, that.myPosition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameDesc, owner, myPosition, myTurns);
    }

    @Override
    public String toString() {
        return "InformationSet{" +
                "gameDesc=" + gameDesc +
                ", owner=" + owner +
                ", myPosition=" + myPosition +
                ", myTurns=" + myTurns +
                '}';
    }
}
