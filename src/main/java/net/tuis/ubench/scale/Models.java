package net.tuis.ubench.scale;

import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;

/**
 * Container of standard Scaling models.
 * 
 * @author Simon Forsberg
 */
public class Models {

    /**
     * Represents O(a<sup>n</sup>)
     */
    public static final MathModel EXPONENTIAL = new MathModel("O(a^n)", "%f ^ n",
            params -> x -> Math.pow(params[0], x), new double[] { 2 });

    /**
     * Represents O(n log n)
     */
    public static final MathModel N_LOG_N = new MathModel("O(n log n)", "%f * n log n",
            params -> x -> params[0] * x * Math.log10(x), new double[] { 1 });

    /**
     * Represents O(log n)
     */
    public static final MathModel LOG_N = new MathModel("O(log n)", "%f * log n",
            params -> x -> params[0] * Math.log10(x), new double[] { 1 });

    /**
     * Represents O(1)
     */
    public static final MathModel CONSTANT = createPolynom(0);

    /**
     * Represents O(n)
     */
    public static final MathModel LINEAR = createPolynom(1);

    /**
     * Represents O(n<sup>2</sup>)
     */
    public static final MathModel N_SQUARED = createPolynom(2);

    /**
     * Create an n<sup>th</sup> degree polynomial model.
     * 
     * e.g. <code>createPolynom(4)</code> would create an O(n<sup>4</sup>)
     * model.
     * 
     * @param degree
     *            the polynomial degree
     * @return a MathModel representing the specified degree.
     */
    public static MathModel createPolynom(int degree) {
        if (degree < 0) {
            throw new IllegalArgumentException("Degree must be positive");
        }
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
        String name;
        switch (degree) {
            case 0:
                name = "O(1)";
                break;
            case 1:
                name = "O(n)";
                break;
            default:
                name = "O(n^" + degree + ")";
                break;
        }
        return new MathModel(name, format.toString(), equation, params);
    }

}
