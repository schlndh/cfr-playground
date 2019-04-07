package com.ggp.utils;

import com.ggp.IAction;

import java.util.Objects;

/**
 * Action wrapper that remembers the action index and equals to the underlying action.
 */
public class ActionIdxWrapper implements IAction {
    private final IAction action;
    private final int idx;

    public ActionIdxWrapper(IAction action, int idx) {
        this.action = action;
        this.idx = idx;
    }

    public IAction getAction() {
        return action;
    }

    public int getIdx() {
        return idx;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActionIdxWrapper that = (ActionIdxWrapper) o;
        return idx == that.idx &&
                Objects.equals(action, that.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, idx);
    }

    @Override
    public String toString() {
        return Objects.toString(action);
    }
}
