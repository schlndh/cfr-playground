package com.ggp;

import java.io.Serializable;

public interface IInfoSetStrategy extends Serializable {
    double getProbability(int actionIdx);
    int size();
}
