package com.ggp.games.PrincessAndMonster;

import com.ggp.ICompleteInformationState;
import com.ggp.IGameDescription;
import com.ggp.games.PrincessAndMonster.graphs.Grid3x3;

import java.util.Objects;

public class GameDescription implements IGameDescription {
    private static final long serialVersionUID = 1L;
    public static final int PRINCESS = 1;
    public static final int MONSTER = 2;
    private final int maxTime;
    private final IGraph graph = new Grid3x3();
    private final CompleteInformationState initialState;

    public GameDescription(int maxTime) {
        this.maxTime = maxTime;
        initialState = new CompleteInformationState(
                new InformationSet(this, 1, graph.getInitialPosition(1), 0),
                new InformationSet(this, 2, graph.getInitialPosition(2), 0)
                );
    }

    @Override
    public ICompleteInformationState getInitialState() {
        return initialState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameDescription that = (GameDescription) o;
        return maxTime == that.maxTime &&
                Objects.equals(graph, that.graph);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxTime, graph);
    }

    @Override
    public String toString() {
        return "PrincessAndMonster{" +
                    maxTime +
                '}';
    }

    @Override
    public String getConfigString() {
        return toString();
    }

    public int getMaxTime() {
        return maxTime;
    }

    public IGraph getGraph() {
        return graph;
    }
}
