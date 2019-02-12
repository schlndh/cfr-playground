package com.ggp;

public interface IGameListener {
    void gameStart(IPlayer player1, IPlayer player2);
    void gameEnd(int payoff1, int payoff2);
    void stateReached(ICompleteInformationState s);
    void actionSelected(ICompleteInformationState s, IAction a);
}
