package com.ggp.players.solving;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class ObjectTree<T> {
    private HashMap<T, ObjectTree<T>> paths = new HashMap<>();

    private ObjectTree(ObjectTree<T> other) {
        for (Map.Entry<T, ObjectTree<T>> entry: other.paths.entrySet()) {
            paths.put(entry.getKey(), entry.getValue().copy());
        }
    }

    public ObjectTree() {
    }

    public Iterable<T> getPossibleSteps() {
        return paths.keySet();
    }

    public void addPath(Iterable<T> path) {
        if (path == null) return;
        Iterator<T> it = path.iterator();
        if (!it.hasNext()) return;
        ObjectTree<T> set = this;
        do {
            T step = it.next();
            set = set.paths.computeIfAbsent(step, k -> new ObjectTree<>());
        } while (it.hasNext());
    }

    public ObjectTree<T> copy() {
        return new ObjectTree(this);
    }

    public ObjectTree<T> getNext(T step) {
        return paths.getOrDefault(step, null);
    }

    public int size() {
        return paths.size();
    }
}
