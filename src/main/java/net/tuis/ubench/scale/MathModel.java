package net.tuis.ubench.scale;

import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;

/**
 * A MathModel describes an abstract scaling function that could massaged to fit
 * the actual scaling of empirical data.
 * 
 * @author Simon Forsberg
 */
public class MathModel {

    private final Function<double[], DoubleUnaryOperator> function;
    private final double[] initialValues;
    private final String format;
    private final String name;

    /**
     * Create a MathModel to describe a scaling function.
     * 
     * @param name
     *            the name of the model
     * @param format
     *            the format for displaying the coefficients and variables in
     *            the model
     * @param math
     *            the function that converts the supplied array of coefficients
     *            in to a concrete instance of this model represented as a
     *            function too.
     * @param initialValues
     *            sane initial values for estimating and fitting the function
     *            against actual data.
     */
    public MathModel(String name, String format, Function<double[], DoubleUnaryOperator> math, double[] initialValues) {
        this.name = name;
        this.function = math;
        this.initialValues = initialValues;
        this.format = format;
    }

    /**
     * Get the initial values useful as a starting point for applying this
     * function to actual results.
     * 
     * @return the coefficients to start analysis with.
     */
    public double[] getInitialValues() {
        return Arrays.copyOf(initialValues, initialValues.length);
    }

    /**
     * Converts an array of coefficients in to a concrete function
     * 
     * @param params
     *            The coefficents to use
     * @return a function that applies this model using the supplied
     *         coefficients.
     */
    public DoubleUnaryOperator createFunction(double[] params) {
        return function.apply(params);
    }

    /**
     * Get a format string that displays the coefficients in a way that
     * represents this model
     * 
     * @return a String.format template representing this model.
     */
    public String getFormat() {
        return format;
    }

    /**
     * Get the name this model was created with.
     * 
     * @return the Model name.
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return format;
    }
}
