package net.tuis.ubench.scale;

import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;

/**
 * @author Simon Forsberg
 */
public class MathModel {

    private final Function<double[], DoubleUnaryOperator> function;
    private final double[] initialValues;
    private final String format;

    public MathModel(String format, Function<double[], DoubleUnaryOperator> math, double[] initialValues) {
        this.function = math;
        this.initialValues = initialValues;
        this.format = format;
    }

    public double[] getInitialValues() {
        return Arrays.copyOf(initialValues, initialValues.length);
    }

    public Function<double[], DoubleUnaryOperator> getFunction() {
        return function;
    }

    public String getFormat() {
        return format;
    }
}
