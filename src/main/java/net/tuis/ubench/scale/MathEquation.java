package net.tuis.ubench.scale;

import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

/**
 * @author Simon Forsberg
 */
public class MathEquation {

    private final DoubleUnaryOperator equation;
    private final double[] parameters;
    private final String format;
    private final double rSquared;
    private final MathModel model;

    public MathEquation(MathModel model, DoubleUnaryOperator equation, double[] parameters, String format, double rSquared) {
        this.model = model;
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

    public MathModel getModel() {
        return model;
    }

    @Override
    public String toString() {
        Object[] params = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            params[i] = parameters[i];
        }
        return String.format(format, params) + " with precision " + rSquared;
    }

    public String toJSONString() {
        String parms = DoubleStream.of(parameters)
                .mapToObj(d -> String.format("%f", d))
                .collect(Collectors.joining(", ", "[", "]"));
        
        String desc = String.format(format, DoubleStream.of(parameters).mapToObj(Double::valueOf).toArray()); 
        return String.format("{name: \"%s\", description: \"%s\", parameters: %s, rsquare: %f}", 
                format, desc, parms, rSquared);
    }

    public boolean isValid() {
        return Math.abs(parameters[0]) >= 0.001;
    }
}
