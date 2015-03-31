package net.tuis.ubench;

import net.tuis.ubench.scale.MathEquation;
import net.tuis.ubench.scale.MathModel;
import net.tuis.ubench.scale.Models;
import org.apache.commons.math3.linear.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;

/**
 * @author Simon Forsberg
 */
public class ScaleDetect {

    private static final double TOLERANCE = 1e-4;
    private static final double H = 1e-5;

    /**
     * Finding the best fit using least-squares method for an equation system
     *
     * @param function Equation system to find fit for. Input: Parameters, Output: Residuals.
     * @param initial Initial 'guess' for function parameters
     * @param tolerance How much the function parameters may change before a solution is accepted
     * @return The parameters to the function that causes the least residuals
     */
    private static double[] newtonSolve(Function<double[], double[]> function, double[] initial, double tolerance) {
        RealVector dx = new ArrayRealVector(initial.length);
        dx.set(tolerance + 1);
        int iterations = 0;
        int d = initial.length;
        double[] values = Arrays.copyOf(initial, initial.length);

        while (dx.getNorm() > tolerance) {
            double[] fx = function.apply(values);
            Array2DRowRealMatrix df = new Array2DRowRealMatrix(fx.length, d);
            ArrayRealVector fxVector = new ArrayRealVector(fx);
            for (int i = 0; i < d; i++) {
                double originalValue = values[i];
                values[i] += H;
                double[] fxi = function.apply(values);
                values[i] = originalValue;
                ArrayRealVector fxiVector = new ArrayRealVector(fxi);
                RealVector result = fxiVector.subtract(fxVector);
                result = result.mapDivide(H);
                df.setColumn(i, result.toArray());
            }
            dx = new RRQRDecomposition(df).getSolver().solve(fxVector.mapMultiply(-1));
            // df has size = initial, and fx has size equal to whatever that function produces.
            for (int i = 0; i < values.length; i++) {
                values[i] += dx.getEntry(i);
            }
            iterations++;
            if (iterations % 100 == 0) {
                tolerance *= 10;
            }
        }
        System.out.println("solved in " + iterations + " iterations");
        return values;
    }

    public static MathEquation detect(UScale scale) {
        return Arrays.stream(rank(scale)).filter(eq -> eq.isValid()).findFirst().get();
    }

    public static MathEquation[] rank(UScale scale) {
        List<UStats> stats = scale.getStats();

        double[] x = new double[stats.size()];
        double[] y = new double[stats.size()];
        for (int i = 0; i < stats.size(); i++) {
            UStats stat = stats.get(i);
            x[i] = stat.getIndex();
            y[i] = stat.getAverageRawNanos();
        }
        return rank(x, y);
    }

    private static MathEquation[] rank(double[] x, double[] y) {
        MathModel[] models = new MathModel[]{ Models.LINEAR, Models.CONSTANT, Models.LOG_N, Models.N_LOG_N, Models.N_SQUARED };
        // sort by reverse rsquared, or negative r-squared... note the `-` in `eq -> - eq.getRSquared()`
        return Arrays.stream(models).map(m -> detect(x, y, m)).sorted(Comparator.comparingDouble(eq -> - eq.getRSquared())).toArray(size -> new MathEquation[size]);
    }

    private static MathEquation detect(double[] x, double[] y, MathModel model) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("x and y size must match");
        }

        Function<double[], double[]> function = new Function<double[], double[]>() {
            @Override
            public double[] apply(double[] doubles) {
                double[] result = new double[x.length];
                DoubleUnaryOperator func = model.createFunction(doubles);
                for (int i = 0; i < x.length; i++) {
                    result[i] = y[i] - func.applyAsDouble(x[i]);
                }
                return result;
            }
        };
        double[] results = newtonSolve(function, model.getInitialValues(), TOLERANCE);
        DoubleUnaryOperator finalFunction = model.createFunction(results);

        double rSquared = calculateRSquared(finalFunction, x, y);
        MathEquation eq = new MathEquation(finalFunction, results, model.getFormat(), rSquared);
        System.out.println(Arrays.toString(results));
        System.out.println(eq);
        System.out.println();
        return new MathEquation(finalFunction, results, model.getFormat(), rSquared);
    }

    private static double calculateRSquared(DoubleUnaryOperator finalFunction, double[] x, double[] y) {
        double yAverage = Arrays.stream(y).average().getAsDouble();
        double variance = 0;
        double residualSumOfSquares = 0;
        double explainedSumOfSquares = 0;

        for (int i = 0; i < y.length; i++) {
            double yi = y[i];
            double fi = finalFunction.applyAsDouble(x[i]);
            variance += (yi - yAverage) * (yi - yAverage);
            residualSumOfSquares += (yi - fi) * (yi - fi);
            explainedSumOfSquares += (fi - yAverage) * (fi - yAverage);
        }

        double rSquared = 1 - residualSumOfSquares / variance;
        System.out.println(Arrays.toString(x));
        System.out.println(Arrays.toString(y));
        System.out.println(String.format("%f variance, %f residual sum, %f avg, %f rsquared, %f explained sum of squares",
                variance, residualSumOfSquares, yAverage, rSquared, explainedSumOfSquares));
        return rSquared;
    }

    public static void main(String[] args) {
        detect(new double[]{ 42, 107, 73, 120 }, new double[]{ 511, 312, 400, 242 }, Models.N_SQUARED);

        detect(new double[]{ 1.0, 2.0, 4.0, 8.0, 16.0, 32.0, 64.0, 128.0, 256.0, 512.0, 1024.0, 2048.0, 4096.0, 8192.0, 16384.0, 32768.0, 65536.0, 131072.0, 262144.0, 524288.0 },
                new double[]{ 905.0, 901.0, 939.0, 927.0, 920.0, 898.0, 884.0, 861.0, 852.0, 864.0, 869.0, 867.0, 866.0, 867.0, 857.0, 857.0, 854.0, 855.0, 872.0, 865.0 },
                Models.LINEAR);

        detect(new double[]{ 1.0, 2.0, 4.0, 8.0, 16.0, 32.0, 64.0, 128.0, 256.0, 512.0, 1024.0, 2048.0, 4096.0, 8192.0, 16384.0, 32768.0, 65536.0, 131072.0, 262144.0, 524288.0 },
               new double[]{ 857.0, 860.0, 898.0, 975.0, 993.0, 1601.0, 1530.0, 2947.0, 6106.0, 16111.0, 35937.0, 80497.0, 184819.0, 390424.0, 847658.0, 1820366.0, 4095873.0, 8463674.0, 17483933, 39126742 },
                Models.N_LOG_N);

        detect(new double[]{ 4, 10, 15 },
                new double[]{ 10.1146, 42, 74.0937 },
                Models.N_LOG_N);
    }

}
