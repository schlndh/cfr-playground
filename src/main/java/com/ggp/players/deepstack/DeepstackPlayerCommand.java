package com.ggp.players.deepstack;

import com.ggp.IGameListener;
import com.ggp.IPlayerFactory;
import com.ggp.cli.IPlayerFactoryCommand;
import com.ggp.players.deepstack.debug.ResolvingListener;
import com.ggp.players.deepstack.regret_matching.RegretMatchingPlus;
import com.ggp.players.deepstack.resolvers.ExternalCFRResolver;
import com.ggp.solvers.cfr.DepthLimitedCFRSolver;
import picocli.CommandLine;

@CommandLine.Command(
        name = "deepstack",
        description = "Deepstack player"
)
public class DeepstackPlayerCommand implements IPlayerFactoryCommand {
    private ResolvingListener listener = new ResolvingListener();

    @Override
    public IPlayerFactory getPlayerFactory() {
        return new DeepstackPlayer.Factory(new ExternalCFRResolver.Factory(new DepthLimitedCFRSolver.Factory(new RegretMatchingPlus.Factory())), listener);
    }

    @Override
    public IGameListener getGameListener() {
        return listener;
    }
}
