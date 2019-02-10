package com.ggp.solvers.cfr.is_info;

import com.ggp.solvers.cfr.IRegretMatching;
import com.ggp.solvers.cfr.IBaseline;
import com.ggp.utils.PlayerHelpers;

import java.util.Arrays;

public class MCCFRISInfo extends BaseCFRISInfo {
    private IBaseline baseline1, baseline2;
    private long lastVisitedAtIteration;

    private MCCFRISInfo(MCCFRISInfo info) {
        super(info);
        this.baseline1 = info.baseline1.copy();
        this.baseline2 = info.baseline2.copy();
        this.lastVisitedAtIteration = info.lastVisitedAtIteration;
    }

    public MCCFRISInfo(IRegretMatching.IFactory rmFactory, int actionSize, IBaseline.IFactory baselineFactory) {
        super(rmFactory, actionSize);
        this.baseline1 = baselineFactory.create(actionSize);
        this.baseline2 = baselineFactory.create(actionSize);
        lastVisitedAtIteration = 0;
    }

    public IBaseline getBaseline(int player) {
        return PlayerHelpers.selectByPlayerId(player, baseline1, baseline2);
    }

    public long getLastVisitedAtIteration() {
        return lastVisitedAtIteration;
    }

    public void setLastVisitedAtIteration(long lastVisitedAtIteration) {
        this.lastVisitedAtIteration = lastVisitedAtIteration;
    }

    @Override
    public BaseCFRISInfo copy() {
        return new MCCFRISInfo(this);
    }
}
