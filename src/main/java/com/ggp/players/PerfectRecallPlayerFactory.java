package com.ggp.players;

import com.ggp.IGameDescription;
import com.ggp.IPlayer;
import com.ggp.IPlayerFactory;
import com.ggp.utils.recall.PerfectRecallGameDescriptionWrapper;

/**
 * Player factory wrapper that makes the game perfect recall.
 */
public class PerfectRecallPlayerFactory implements IPlayerFactory {
    private IPlayerFactory playerFactory;

    @Override
    public IPlayer create(IGameDescription game, int role) {
        return playerFactory.create(new PerfectRecallGameDescriptionWrapper(game), role);
    }

    @Override
    public String toString() {
        return "PerfectRecall{" +
                    playerFactory.getConfigString() +
                '}';
    }

    @Override
    public String getConfigString() {
        return toString();
    }
}
