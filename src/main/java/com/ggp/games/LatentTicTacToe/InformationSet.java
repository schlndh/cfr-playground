package com.ggp.games.LatentTicTacToe;

import com.ggp.IAction;
import com.ggp.IInformationSet;
import com.ggp.IPercept;

import java.io.IOException;
import java.util.*;

public class InformationSet implements IInformationSet{
    private static final long serialVersionUID = 1L;
    static final int GRID_SIZE = 3;
    private static final MarkFieldAction[][] actionCache;
    static {
        actionCache = new MarkFieldAction[GRID_SIZE][GRID_SIZE];
        for (int x = 0; x < GRID_SIZE; ++x) {
            for (int y = 0; y < GRID_SIZE; ++y) {
                actionCache[x][y] = new MarkFieldAction(x, y);
            }
        }
    }
    private int[] field;
    private int owningPlayerId;
    private int turn;
    private int myFields;
    private int knownEnemyFields;
    private MarkFieldAction delayedAction;
    private Long comparisonKey;
    public static final int FIELD_UNKNOWN = 0;
    public static final int FIELD_MINE = 1;
    public static final int FIELD_ENEMY = 2;

    public InformationSet(int[] field, int owningPlayerId, int turn, int myFields, int knownEnemyFields, MarkFieldAction delayedAction) {
        this.field = field;
        this.owningPlayerId = owningPlayerId;
        this.turn = turn;
        this.myFields = myFields;
        this.knownEnemyFields = knownEnemyFields;
        this.delayedAction = delayedAction;

        initComparisonKey();
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        initComparisonKey();
    }

    public boolean hasLegalActions() {
        return myFields + knownEnemyFields + (delayedAction != null ? 1 : 0) < field.length;
    }

    @Override
    public List<IAction> getLegalActions() {
        ArrayList<IAction> ret = new ArrayList<>(field.length - myFields - knownEnemyFields);
        if (!hasLegalActions()) return ret;
        for(int x = 0; x < GRID_SIZE; ++x) {
            for (int y = 0; y < GRID_SIZE; ++y) {
                if (actionCache[x][y].equals(delayedAction)) continue;
                if (field[GRID_SIZE*x + y] == FIELD_UNKNOWN) ret.add(actionCache[x][y]);
            }
        }
        return ret;
    }

    @Override
    public IInformationSet next(IAction a) {
        if (!isLegal(a)) return null;
        return new InformationSet(field, owningPlayerId, turn + 1, myFields, knownEnemyFields, (MarkFieldAction) a);
    }

    @Override
    public IInformationSet applyPercept(IPercept p) {
        if (!isValid(p)) return null;
        MarkFieldAction opponentsDelayedAction = ((DelayedActionPercept) p).getDelayedAction();
        int[] f = Arrays.copyOf(field, field.length);
        int myNextFields = myFields;
        int nextEnemyFields = knownEnemyFields;
        if (opponentsDelayedAction != null) {
            int delayedPos = GRID_SIZE * opponentsDelayedAction.getX() + opponentsDelayedAction.getY();
            if (f[delayedPos] == FIELD_UNKNOWN) {
                f[delayedPos] = FIELD_ENEMY;
                nextEnemyFields++;
            }
        }
        int delayedPos = GRID_SIZE * delayedAction.getX() + delayedAction.getY();
        if (field[delayedPos] == FIELD_UNKNOWN) {
            f[delayedPos] = FIELD_MINE;
            myNextFields++;
        }

        return new InformationSet(f, owningPlayerId, turn + 1, myNextFields, nextEnemyFields, delayedAction);
    }

    @Override
    public boolean isLegal(IAction a) {
        if (a == null || !(a instanceof MarkFieldAction)) return false;
        MarkFieldAction _a = (MarkFieldAction) a;
        int x = _a.getX();
        int y = _a.getY();
        if (x < 0 || x >= GRID_SIZE || y < 0 || y >= GRID_SIZE) return false;
        if (field[GRID_SIZE*x + y] != FIELD_UNKNOWN) return false;
        return true;
    }

    public boolean hasPlayerWon() {
        if (myFields < GRID_SIZE) return false;
        // check columns
        for(int x = 0; x < GRID_SIZE; ++x) {
            int y = 0;
            for(; y < GRID_SIZE; ++y) {
                if (field[GRID_SIZE*x + y] != FIELD_MINE) break;
            }
            if (y == GRID_SIZE) return true;
        }

        // check rows
        for(int y = 0; y < GRID_SIZE; ++y) {
            int x = 0;
            for(; x < GRID_SIZE; ++x) {
                if (field[GRID_SIZE*x + y] != FIELD_MINE) break;
            }
            if (x == GRID_SIZE) return true;
        }

        // check diagonals
        {
            int i = 0;
            for(; i < GRID_SIZE; ++i) {
                if (field[i*GRID_SIZE + i] != FIELD_MINE) break;
            }
            if (i == GRID_SIZE) return true;
        }
        {
            int i = 0;
            for(; i < GRID_SIZE; ++i) {
                if (field[i*GRID_SIZE + (GRID_SIZE - 1 - i)] != FIELD_MINE) break;
            }
            if (i == GRID_SIZE) return true;
        }
        return false;
    }

    private void initComparisonKey() {
        long sum = owningPlayerId;
        long mul = 3;
        for (int i = 0; i < GRID_SIZE*GRID_SIZE; ++i) {
            sum += field[i] * mul;
            mul *= 3;
        }
        if (owningPlayerId == 2) sum = -sum;
        comparisonKey = sum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InformationSet that = (InformationSet) o;
        return Objects.equals(comparisonKey, that.comparisonKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(comparisonKey);
    }

    @Override
    public boolean isValid(IPercept p) {
        if (p == null || p.getClass() != DelayedActionPercept.class || p.getTargetPlayer() != owningPlayerId) return false;
        DelayedActionPercept _p = (DelayedActionPercept) p;
        MarkFieldAction a = _p.getDelayedAction();
        if (a == null) return true;
        int x = a.getX(), y = a.getY();
        return x >= 0 && x < GRID_SIZE && y >= 0 && y < GRID_SIZE;
    }

    @Override
    public int getOwnerId() {
        return owningPlayerId;
    }

    public MarkFieldAction getDelayedAction() {
        return delayedAction;
    }

    @Override
    public String toString() {
        return "IS{" +
                "owner=" + owningPlayerId +
                ", turn=" + turn +
                ", myFields=" + myFields +
                ", knownEnemyFields=" + knownEnemyFields +
                ", delayedAction=" + delayedAction +
                '}';
    }
}
