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
    private final String name;

    public MathModel(String name, String format, Function<double[], DoubleUnaryOperator> math, double[] initialValues) {
        this.name = name;
        this.function = math;
        this.initialValues = initialValues;
        this.format = format;
    }

    public double[] getInitialValues() {
        return Arrays.copyOf(initialValues, initialValues.length);
    }

    public DoubleUnaryOperator createFunction(double[] params) {
        return function.apply(params);
    }

    public String getFormat() {
        return format;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return format;
    }
}
