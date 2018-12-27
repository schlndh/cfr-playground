package com.ggp.games.PrincessAndMonster;

import com.ggp.IAction;

import java.io.Serializable;
import java.util.List;

/**
 * Game graph
 */
public interface IGraph extends Serializable {
    IGraphPosition getInitialPosition(int player);
    List<IAction> getLegalMoves(IGraphPosition position);
    boolean isLegal(IGraphPosition position, IAction move);
}
