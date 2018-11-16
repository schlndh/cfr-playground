package com.ggp.utils;

import com.ggp.IGameDescription;

public class GameRepository {
    public static IGameDescription leducPoker(int money1, int money2) {
        return new com.ggp.games.LeducPoker.GameDescription(money1, money2);
    }

    public static IGameDescription iiGoofspiel(int n) {
        return new com.ggp.games.IIGoofspiel.GameDescription(n);
    }

    public static IGameDescription kriegTTT() {
        return new com.ggp.games.TicTacToe.GameDescription();
    }

    public static IGameDescription rps(int n) {
        return new com.ggp.games.RockPaperScissors.GameDescription(n);
    }
}
