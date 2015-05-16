package com.mweagle.tereus;

import java.util.function.Function;

/**
 * Created by mweagle on 5/12/15.
 */
public class Mu {

    public static <I, O> Function<I, O> wrappedFunction(Function<I, O> wrapMe)
    {
        return input ->
        {
            try
            {
                return wrapMe.apply(input);
            }
            catch (RuntimeException ex)
            {
                throw new RuntimeException(ex);
            }
        };
    }
}
