package com.ggp.utils;

import com.ggp.IGameDescription;

public class GameRepository {
    public static IGameDescription leducPoker(int money) {
        return new com.ggp.games.LeducPoker.GameDescription(money);
    }

    public static IGameDescription leducPoker(int money1, int money2, int betsPerRound, int cardsPerSuite) {
        return new com.ggp.games.LeducPoker.GameDescription(money1, money2, betsPerRound, cardsPerSuite);
    }

    public static IGameDescription iiGoofspiel(int n) {
        return new com.ggp.games.IIGoofspiel.GameDescription(n);
    }

    public static IGameDescription rps(int n) {
        return new com.ggp.games.RockPaperScissors.GameDescription(n);
    }

    public static IGameDescription princessAndMonster(int maxTurns) {
        return new com.ggp.games.PrincessAndMonster.GameDescription(maxTurns);
    }

    public static IGameDescription latentTTT() {
        return new com.ggp.games.LatentTicTacToe.GameDescription();
    }
}
