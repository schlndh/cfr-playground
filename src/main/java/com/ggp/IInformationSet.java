package com.ggp;

import java.io.Serializable;
import java.util.List;

/**
 * Information set interface.
 *
 * IS I is in acting state when obtained from CIS by calling getInfoSetForActingPlayer or there exists
 * a path from CIS s (initial state or owner is the acting player) to CIS t (owner is the acting player) such that
 * starting with J = s.getInfoSetForPlayer(owner) and sequentially updating J with owner's actions and percepts on path to t
 * leads to I.
 *
 * Corresponds to augmented information set when in non-acting state.
 * Methods dealing with actions have undefined  behavior when in non-acting state.
 *
 * Implementing class must be immutable and must suitably override hashCode and equals methods.
 */
public interface IInformationSet extends Serializable {
    /**
     * Generate information set after taking an action.
     *
     * Undefined behavior in non-acting state.
     * @param a action
     * @return new IS or null if action is not legal.
     */
    IInformationSet next(IAction a);

    /**
     * Generate information set after receiving a percept.
     * @param p percept
     * @return new IS, must return null if given percept cannot occur in this IS
     */
    IInformationSet applyPercept(IPercept p);

    /**
     * Get list of legal actions.
     *
     * Undefined behavior in non-acting state.
     * @return list or null if there are no legal actions (IS is terminal).
     */
    List<IAction> getLegalActions();

    /**
     * Check whether given action is legal in this IS.
     *
     * Undefined behavior in non-acting state.
     * @param a
     * @return
     */
    boolean isLegal(IAction a);

    /**
     * Check whether player can receive given percept in this IS.
     * @param p
     * @return
     */
    boolean isValid(IPercept p);

    /**
     * Get ID of player owning this IS
     * @return
     */
    int getOwnerId();
}
