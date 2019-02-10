package com.ggp.player_evaluators;

import com.ggp.*;
import com.ggp.players.deepstack.ISubgameResolver;

public interface IEvaluablePlayer extends IPlayer {
    interface IFactory extends IPlayerFactory {
        @Override
        IEvaluablePlayer create(IGameDescription game, int role);

        void registerResolvingListener(IListener listener);

        void unregisterResolvingListener(IListener listener);
    }

    interface IResolvingInfo {
        /**
         * Get normalized subgame strategy.
         *
         * Must contain strategy for current IS.
         * Strategy for all contained IS should be approximately equal to strategy used by the player in given IS.
         * @return
         */
        IStrategy getNormalizedSubgameStrategy();

        /**
         * Get player's current IS.
         * @return
         */
        IInformationSet getCurrentInfoSet();

        /**
         * Get number of states visited in current resolving.
         * @return
         */
        long getVisitedStatesInCurrentResolving();
    }

    interface IListener {
        void initEnd(IResolvingInfo resInfo);
        void resolvingStart(IResolvingInfo resInfo);
        void resolvingEnd(IResolvingInfo resInfo);
        void resolvingIterationEnd(IResolvingInfo resInfo);
    }

    void registerResolvingListener(IListener listener);

    void unregisterResolvingListener(IListener listener);

    /**
     * Get deep copy of the player.
     * @return
     */
    IEvaluablePlayer copy();


    /**
     * Compute strategy for current IS.
     * @param timeoutMillis
     */
    void computeStrategy(long timeoutMillis);


    /**
     * Advance to next state by taking given action.
     *
     * This method must only be called after computeStrategy.
     * @param selectedAction
     */
    void actWithPrecomputedStrategy(IAction selectedAction);

    /**
     * Get normalized subgame strategy.
     *
     * Works like {@link IResolvingInfo#getNormalizedSubgameStrategy() in IResolvingInfo}.
     * @return
     */
    IStrategy getNormalizedSubgameStrategy();
}
