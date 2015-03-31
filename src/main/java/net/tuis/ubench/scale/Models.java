package net.tuis.ubench.scale;

import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;

/**
 * @author Simon Forsberg
 */
public class Models {

    public static final MathModel EXPONENTIAL = new MathModel("%f ^ n", params -> x -> Math.pow(params[0], x), new double[]{ 2 });

    public static final MathModel N_LOG_N = new MathModel("%f * n log n", params -> x -> params[0] * x * Math.log10(x), new double[]{ 1 });

    public static final MathModel LOG_N = new MathModel("%f * log n", params -> x -> params[0] * Math.log10(x), new double[]{ 1 });

    public static final MathModel CONSTANT = createPolynom(0);
    public static final MathModel LINEAR = createPolynom(1);
    public static final MathModel N_SQUARED = createPolynom(2);


    public static MathModel createPolynom(int degree) {
        double[] params = new double[degree + 1];
        params[0] = 1;
        StringBuilder format = new StringBuilder();
        for (int i = degree; i >= 0; i--) {
            if (i > 0) {
                format.append("%f*n");
                if (i > 1) {
                    format.append('^');
                    format.append(i);
                }
                format.append(" + ");
            }
        }
        format.append("%f");
        Function<double[], DoubleUnaryOperator> equation = new Function<double[], DoubleUnaryOperator>() {
            @Override
            public DoubleUnaryOperator apply(double[] doubles) {
                return x -> {
                    double sum = 0;
                    for (int i = degree; i >= 0; i--) {
                        sum += doubles[degree - i] * Math.pow(x, i);
                    }
                    return sum;
                };
            }
        };
        return new MathModel(format.toString(), equation, params);
    }

    private static DoubleUnaryOperator nSquared(double a, double b, double c) {
        return x -> a * x * x + b * x + c;
    }

}
