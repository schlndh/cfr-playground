package com.ggp.utils;

import com.ggp.ICompleteInformationState;
import com.ggp.IInformationSet;
import com.ggp.IStateVisualizer;

public class DefaultStateVisualizer implements IStateVisualizer {
    @Override
    public void visualize(IInformationSet s, int role) {
        System.out.println(s.toString());
    }

    @Override
    public void visualize(ICompleteInformationState s) {
        System.out.println(s.toString());
        if (s.isTerminal()) {
            System.out.println(String.format("Finished: 1(%d), 2(%d)", (int)s.getPayoff(1), (int)s.getPayoff(2)));
        }
    }
}
