package com.ggp.games.PrincessAndMonster.graphs;

import com.ggp.IAction;
import com.ggp.games.PrincessAndMonster.GameDescription;
import com.ggp.games.PrincessAndMonster.IGraph;
import com.ggp.games.PrincessAndMonster.IGraphPosition;

import java.lang.reflect.Array;
import java.util.*;

public class Grid3x3 implements IGraph {
    private static final long serialVersionUID = 1L;
    public static class Position implements IGraphPosition {
        private static final long serialVersionUID = 1L;
        private final int x, y;

        public Position(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Position position = (Position) o;
            return x == position.x &&
                    y == position.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }

        @Override
        public String toString() {
            return "Position{" +
                        x +
                    "," + y +
                    '}';
        }
    }

    private static final Position[][] positions;
    private static final List[][] legalMoves;

    static {
        positions = new Position[3][3];
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                positions[x][y] = new Position(x, y);
            }
        }
        legalMoves = new List[3][3];
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                ArrayList<IAction> moves = new ArrayList<>(4);
                if (x > 0) moves.add(positions[x-1][y]);
                if (x < 2) moves.add(positions[x+1][y]);
                if (y > 0) moves.add(positions[x][y-1]);
                if (y < 2) moves.add(positions[x][y+1]);
                legalMoves[x][y] = Collections.unmodifiableList(moves);
            }
        }
    }

    public static Position getPosition(int x, int y) {
        if (checkPosition(x, y)) return positions[x][y];
        return null;
    }

    private static boolean checkPosition(int x, int y) {
        return x >= 0 && x < 3 && y >= 0 && y < 3;
    }

    @Override
    public IGraphPosition getInitialPosition(int player) {
        if (player == GameDescription.PRINCESS) return positions[0][0];
        if (player == GameDescription.MONSTER) return positions[2][2];
        return null;
    }

    @Override
    public List<IAction> getLegalMoves(IGraphPosition position) {
        Position pos = (Position) position;
        if (checkPosition(pos.getX(), pos.getY()))
            return legalMoves[pos.getX()][pos.getY()];
        return null;
    }

    @Override
    public boolean isLegal(IGraphPosition position, IAction move) {
        return getLegalMoves(position).contains(move);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return true;
    }
}
