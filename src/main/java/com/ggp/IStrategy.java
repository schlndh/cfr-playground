package com.ggp;

import java.io.Serializable;

public interface IStrategy extends Serializable {
    Iterable<IInformationSet> getDefinedInformationSets();
    boolean isDefined(IInformationSet is);
    IInfoSetStrategy getInfoSetStrategy(IInformationSet is);

    /**
     * Shortcut method to get action probabilty for given IS
     * @param is
     * @param actionIdx
     * @return
     */
    default double getProbability(IInformationSet is, int actionIdx) {
        if (isDefined(is)) {
            return getInfoSetStrategy(is).getProbability(actionIdx);
        } else {
            return 1d/is.getLegalActions().size();
        }
    }
}
