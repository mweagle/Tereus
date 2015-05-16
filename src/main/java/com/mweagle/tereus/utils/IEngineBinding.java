package com.mweagle.tereus.utils;

import java.util.Map;

/**
 * Created by mweagle on 4/26/15.
 */
public interface IEngineBinding {
    public String getBindingName();

    default public Map<String, Object> getEvaluationResult() {
        return null;
    }
}
