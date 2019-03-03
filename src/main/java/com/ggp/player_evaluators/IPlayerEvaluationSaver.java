package com.ggp.player_evaluators;

import java.io.IOException;

public interface IPlayerEvaluationSaver {
    void add(EvaluatorEntry e, double exploitability) throws IOException;
    void close() throws IOException;
}
