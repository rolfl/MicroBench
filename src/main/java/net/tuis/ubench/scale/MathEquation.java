package net.tuis.ubench.scale;

import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

/**
 * A function that describes one of the standard scaling equations
 * 
 * @author Simon Forsberg
 */
public class MathEquation {

    private final DoubleUnaryOperator equation;
    private final double[] parameters;
    private final String format;
    private final double rSquared;
    private final MathModel model;

    /**
     * A function that describes one of the standard scaling equations
     * 
     * @param model
     *            the model this function is based on
     * @param equation
     *            the x-to-y equation for this instance
     * @param parameters
     *            the parameters describing the required coefficients in the
     *            equation
     * @param format
     *            the string format for the equation
     * @param rSquared
     *            the measure of the accuracy of this equation against the
     *            actual results.
     */
    public MathEquation(MathModel model, DoubleUnaryOperator equation, double[] parameters, String format,
            double rSquared) {
        this.model = model;
        this.equation = equation;
        this.parameters = parameters;
        this.format = format;
        this.rSquared = rSquared;
    }

    /**
     * Get a text-based description of this equation
     * 
     * @return the string version of this equation
     */
    public String getDescription() {
        Object[] params = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            params[i] = parameters[i];
        }
        return String.format(format, params);
    }

    /**
     * Get the parameters representing the various coefficients in this equation
     * 
     * @return a copy of the equation coefficients
     */
    public double[] getParameters() {
        return Arrays.copyOf(parameters, parameters.length);
    }

    /**
     * Get a function representing the x-to-y transform for this eqation
     * 
     * @return an equation transforming an x position to a y offset.
     */
    public DoubleUnaryOperator getEquation() {
        return equation;
    }

    /**
     * A String.format template for presenting the equation with its parameters
     * 
     * @return A String format specification suitable for the parameters.
     */
    public String getFormat() {
        return format;
    }

    /**
     * Get the accuracy measure for this equation against the actual results. A
     * value of 1.0 is a compelte match against the actual results, a value
     * close to zero is a fail-to-match
     * 
     * @return the r-squared value representing this equation's accuracy
     */
    public double getRSquared() {
        return rSquared;
    }

    /**
     * Get the mathematical model this equation is based on.
     * 
     * @return the underlying model.
     */
    public MathModel getModel() {
        return model;
    }

    @Override
    public String toString() {
        return getDescription() + " with precision " + rSquared;
    }

    /**
     * Convert this equation in to a JSON string representing the vital
     * statistics of the equation.
     * 
     * @return a JSON interpretation of this equation.
     */
    public String toJSONString() {
        String parms = DoubleStream.of(parameters)
                .mapToObj(d -> String.format("%f", d))
                .collect(Collectors.joining(", ", "[", "]"));

        String desc = String.format(format, DoubleStream.of(parameters).mapToObj(Double::valueOf).toArray());
        return String.format(
                "{name: \"%s\", valid: %s, format: \"%s\", description: \"%s\", parameters: %s, rsquare: %f}",
                model.getName(), isValid() ? "true" : "false", format, desc, parms, rSquared);
    }

    /**
     * Indicate whether this equation is a suitable match against the actual
     * data.
     * 
     * @return true if this equation is useful when representing the actual
     *         data's scalability
     */
    public boolean isValid() {
        return Math.abs(parameters[0]) >= 0.001 && rSquared != Double.NEGATIVE_INFINITY && !Double.isNaN(rSquared);
    }
}
