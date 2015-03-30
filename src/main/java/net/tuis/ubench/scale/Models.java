package net.tuis.ubench.scale;

import java.util.function.DoubleUnaryOperator;

/**
 * @author Simon Forsberg
 */
public class Models {

    public static final MathModel N_LOG_N = new MathModel("%f * n log n", params -> x -> params[0] * x * Math.log10(x), new double[]{ 1 });

    public static final MathModel N_LOG_N_WITH_CONST = new MathModel("%f * n log n + %f", params -> x -> params[0] * x * Math.log10(x) + params[1], new double[]{ 1, 1 });

    public static final MathModel LINEAR = new MathModel("%f * n + %f", params -> x -> params[0] * x + params[1], new double[]{ 1, 1 });

    public static final MathModel LOG_N = new MathModel("%f * log n", params -> x -> params[0] * Math.log10(x), new double[]{ 1 });

    // CONSTANT will not produce a reasonable rSquared value. Use LINEAR instead and check coefficient.
//    public static final MathModel CONSTANT = new MathModel("%f", params -> x -> params[0], new double[]{ 1 });

    public static final MathModel N_SQUARED = new MathModel("%f*n^2 + %f*n + %f", params -> nSquared(params[0], params[1], params[2]), new double[]{ 1, 0, 0 });

    private static DoubleUnaryOperator nSquared(double a, double b, double c) {
        return x -> a * x * x + b * x + c;
    }

}
