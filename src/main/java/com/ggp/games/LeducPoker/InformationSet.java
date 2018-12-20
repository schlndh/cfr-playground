package com.ggp.games.LeducPoker;

import com.ggp.IAction;
import com.ggp.IInformationSet;
import com.ggp.IPercept;
import com.ggp.games.LeducPoker.actions.CallAction;
import com.ggp.games.LeducPoker.actions.FoldAction;
import com.ggp.games.LeducPoker.actions.RaiseAction;
import com.ggp.games.LeducPoker.percepts.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InformationSet implements IInformationSet {
    private static final long serialVersionUID = 1L;
    private final GameDescription gameDesc;
    private final int owner;
    private final Integer privateCard;
    private final Integer publicCard;
    private final int potSize;
    private final int remainingMoney;
    private final Rounds round;
    private final int raisesUsedThisRound;
    private final int foldedByPlayer;

    public InformationSet(GameDescription gameDesc, int owner, Integer privateCard, Integer publicCard, int potSize, int remainingMoney,
                          Rounds round, int raisesUsedThisRound, int foldedByPlayer
    ) {
        this.gameDesc = gameDesc;
        this.owner = owner;
        this.privateCard = privateCard;
        this.publicCard = publicCard;
        this.potSize = potSize;
        this.remainingMoney = remainingMoney;
        this.round = round;
        this.raisesUsedThisRound = raisesUsedThisRound;
        this.foldedByPlayer = foldedByPlayer;
    }

    @Override
    public IInformationSet next(IAction a) {
        if (!isLegal(a)) return null;
        if (a.getClass() == FoldAction.class) {
            return new InformationSet(gameDesc, owner, privateCard, publicCard, potSize, remainingMoney, Rounds.End, 0, owner);
        } else if (a.getClass() == CallAction.class) {
            int potUpdate = raisesUsedThisRound > 0 ? getRaiseAmount() : 0;
            potUpdate = Math.min(remainingMoney, potUpdate);
            return new InformationSet(gameDesc, owner, privateCard, publicCard, potSize + potUpdate, remainingMoney - potUpdate, round, raisesUsedThisRound, foldedByPlayer);
        } else if (a.getClass() == RaiseAction.class) {
            int potUpdate = getRaiseAmount();
            if (raisesUsedThisRound > 0) potUpdate *= 2;
            return new InformationSet(gameDesc, owner, privateCard, publicCard, potSize + potUpdate, remainingMoney - potUpdate, round, raisesUsedThisRound + 1, foldedByPlayer);
        }
        return null;
    }

    @Override
    public IInformationSet applyPercept(IPercept p) {
        if (!isValid(p)) return null;
        if (p.getTargetPlayer() != owner) return this;
        if (p.getClass() == CardRevealedPercept.class) {
            CardRevealedPercept crp = (CardRevealedPercept) p;
            if (crp.isPublic() && publicCard == null) {
                return new InformationSet(gameDesc, owner, privateCard, crp.getCard(), potSize, remainingMoney, round.next(), 0, foldedByPlayer);
            } else if (!crp.isPublic() && privateCard == null) {
                return new InformationSet(gameDesc, owner, crp.getCard(), publicCard, potSize, remainingMoney, round.next(), 0, foldedByPlayer);
            }
        } else if (p.getClass() == PotUpdatePercept.class) {
            PotUpdatePercept pup = (PotUpdatePercept) p;
            return new InformationSet(gameDesc, owner, privateCard, publicCard, pup.getNewPotSize(), remainingMoney, round, raisesUsedThisRound + 1, foldedByPlayer);
        } else if (p.getClass() == ReturnedMoneyPercept.class) {
            ReturnedMoneyPercept rmp = (ReturnedMoneyPercept) p;
            return new InformationSet(gameDesc, owner, privateCard, publicCard, potSize - rmp.getAmount(), remainingMoney + rmp.getAmount(), round, raisesUsedThisRound, foldedByPlayer);
        } else if (p.getClass() == BettingRoundEndedPercept.class) {
            return new InformationSet(gameDesc, owner, privateCard, publicCard, potSize, remainingMoney, round.next(), 0, foldedByPlayer);
        } else if (p.getClass() == OpponentFoldedPercept.class) {
            return new InformationSet(gameDesc, owner, privateCard, publicCard, potSize, remainingMoney, Rounds.End, 0, owner == 1 ? 2 : 1);
        }
        return null;
    }

    @Override
    public List<IAction> getLegalActions() {
        if (isTerminal()) return null;
        ArrayList<IAction> ret = new ArrayList<>(3);
        if (isLegal(RaiseAction.instance)) {
            ret.add(RaiseAction.instance);
        }
        ret.add(FoldAction.instance);
        ret.add(CallAction.instance);
        return ret;
    }

    @Override
    public boolean isLegal(IAction a) {
        if (isTerminal() || a == null) return false; // terminal
        if (a.getClass() == FoldAction.class || a.getClass() == CallAction.class) return true;
        if (a.getClass() == RaiseAction.class) return raisesUsedThisRound < gameDesc.getBetsPerRound() && remainingMoney >= getRaiseAmount() * (raisesUsedThisRound == 0 ? 1 : 2);
        return false;
    }

    protected int getRaiseAmount() {
        if (round == Rounds.Bet1) return 2;
        if (round == Rounds.Bet2) return 4;
        return 0;
    }

    protected boolean isTerminal() {
        return round == Rounds.End;
    }

    public Integer getPrivateCard() {
        return privateCard;
    }

    public Integer getPublicCard() {
        return publicCard;
    }

    public int getPotSize() {
        return potSize;
    }

    public int getRemainingMoney() {
        return remainingMoney;
    }

    public Rounds getRound() {
        return round;
    }

    public int getMyBets() {
        return gameDesc.getStartingMoney(owner) - remainingMoney;
    }

    public int getOwner() {
        return owner;
    }

    public int getFoldedByPlayer() {
        return foldedByPlayer;
    }

    public int getRaisesUsedThisRound() {
        return raisesUsedThisRound;
    }

    public int getStartingMoney() {
        return gameDesc.getStartingMoney(owner);
    }

    public GameDescription getGameDesc() {
        return gameDesc;
    }

    public boolean wasRaised() {
        return raisesUsedThisRound > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InformationSet that = (InformationSet) o;
        return owner == that.owner &&
                potSize == that.potSize &&
                remainingMoney == that.remainingMoney &&
                raisesUsedThisRound == that.raisesUsedThisRound &&
                foldedByPlayer == that.foldedByPlayer &&
                Objects.equals(gameDesc, that.gameDesc) &&
                privateCard == that.privateCard &&
                publicCard == that.publicCard &&
                round == that.round;
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameDesc, owner, privateCard, publicCard, potSize, remainingMoney, round, raisesUsedThisRound, foldedByPlayer);
    }

    @Override
    public boolean isValid(IPercept p) {
        if (p.getTargetPlayer() != owner || foldedByPlayer != 0) return false;
        if (p.getClass() == CardRevealedPercept.class) {
            CardRevealedPercept crp = (CardRevealedPercept) p;
            return (crp.isPublic() && publicCard == null) || (!crp.isPublic() && privateCard == null);
        } else if (p.getClass() == PotUpdatePercept.class) {
            PotUpdatePercept pup = (PotUpdatePercept) p;
            // the other player may not have enough money to add full raise amount
            return (round == Rounds.Bet1 || round == Rounds.Bet2) && pup.getNewPotSize() <= potSize + 2*getRaiseAmount() && pup.getNewPotSize() >= potSize;
        } else if (p.getClass() == ReturnedMoneyPercept.class) {
            if (raisesUsedThisRound == 0) return false;
            if (round != Rounds.Bet1 && round != Rounds.Bet2) return false;
            ReturnedMoneyPercept rmp = (ReturnedMoneyPercept) p;
            return rmp.getAmount() > 0 && rmp.getAmount() < getRaiseAmount();
        } else if (p.getClass() == BettingRoundEndedPercept.class) {
            return round == Rounds.Bet1 || round == Rounds.Bet2;
        } else if (p.getClass() == OpponentFoldedPercept.class) {
            return (round == Rounds.Bet1 || round == Rounds.Bet2);
        }
        return false;
    }

    @Override
    public int getOwnerId() {
        return owner;
    }

    @Override
    public String toString() {
        return "InformationSet{" +
                "owner=" + owner +
                ", private=" + Objects.toString(privateCard) +
                ", public=" + Objects.toString(publicCard) +
                ", pot=" + potSize +
                ", remainingMoney=" + remainingMoney +
                ", startingMoney=" + gameDesc.getStartingMoney(owner) +
                ", round=" + round +
                ", raises=" + raisesUsedThisRound +
                ", folded=" + foldedByPlayer +
                '}';
    }
}
