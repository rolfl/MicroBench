package net.tuis.ubench.scale;

import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;

/**
 * Created by Simon on 3/27/2015.
 */
public class MathEquation {

    private final DoubleUnaryOperator equation;
    private final double[] parameters;
    private final String format;
    private final double rSquared;

    public MathEquation(DoubleUnaryOperator equation, double[] parameters, String format, double rSquared) {
        this.equation = equation;
        this.parameters = parameters;
        this.format = format;
        this.rSquared = rSquared;
    }

    public String description() {
        return String.format(format, parameters);
    }

    public double[] getParameters() {
        return Arrays.copyOf(parameters, parameters.length);
    }

    public DoubleUnaryOperator getEquation() {
        return equation;
    }

    public String getFormat() {
        return format;
    }

    public double getRSquared() {
        return rSquared;
    }

    @Override
    public String toString() {
        Object[] params = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            params[i] = parameters[i];
        }
        return String.format(format, params) + " with precision " + rSquared;
    }
}
