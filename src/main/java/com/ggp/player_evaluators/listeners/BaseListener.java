package com.ggp.player_evaluators.listeners;

import com.ggp.player_evaluators.IEvaluablePlayer;

public class BaseListener implements IEvaluablePlayer.IListener {
    private boolean initEnded = false;

    protected boolean hasInitEnded() {
        return initEnded;
    }

    @Override
    public void initEnd(IEvaluablePlayer.IResolvingInfo resInfo) {
        initEnded = true;
    }

    @Override
    public void resolvingStart(IEvaluablePlayer.IResolvingInfo resInfo) {
    }

    @Override
    public void resolvingEnd(IEvaluablePlayer.IResolvingInfo resInfo) {
    }

    @Override
    public void resolvingIterationEnd(IEvaluablePlayer.IResolvingInfo resInfo) {
    }

    public void reinit() {
        initEnded = false;
    }
}
