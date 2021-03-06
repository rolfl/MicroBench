package net.tuis.ubench;

import net.tuis.ubench.scale.MathEquation;
import net.tuis.ubench.scale.MathModel;
import net.tuis.ubench.scale.Models;
import org.apache.commons.math3.linear.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;

/**
 * @author Simon Forsberg
 */
public class ScaleDetect {

    private static final double TOLERANCE = 1e-4;
    private static final double H = 1e-5;
    
    private static final Comparator<MathEquation> ranking = Comparator.comparingInt((MathEquation eq) -> eq.isValid() ? 0 : 1)
            .thenComparingDouble(eqb -> - eqb.getRSquared());

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
        return values;
    }

    /**
     * From the results of a UScale run, calculate the best-fit scaling equation
     * @param scale the results to analyze
     * @return the best-fit scaling equation.
     */
    public static MathEquation detect(UScale scale) {
        return Arrays.stream(rank(scale))
                .filter(eq -> eq.isValid())
                .findFirst()
                .orElse(fit(scale, Models.CONSTANT)); // if no valid is found, it is because of constant data
    }

    private static MathEquation fit(UScale scale, MathModel model) {
        return detect(extractX(scale), extractY(scale), model);
    }

    private static double[] extractX(UScale scale) {
        return scale.getStats().stream().mapToDouble(st -> st.getIndex()).toArray();
    }

    private static double[] extractY(UScale scale) {
        return scale.getStats().stream().mapToDouble(st -> st.getFastestNanos()).toArray();
    }

    /**
     * From the scaling results calculate a number of scaling equations, and return their equations in descending order of relevance 
     * @param scale the Scale results to rank
     * @return the possible equations in best-fit-first order.
     */
    public static MathEquation[] rank(UScale scale) {
        double[] x = extractX(scale);
        double[] y = extractY(scale);
        return rank(x, y);
    }

    private static MathEquation[] rank(double[] x, double[] y) {
        MathModel[] models = new MathModel[]{ Models.CONSTANT, Models.LINEAR,
                Models.N_SQUARED, Models.createPolynom(3), Models.createPolynom(4),
                Models.LOG_N, Models.N_LOG_N, Models.EXPONENTIAL };
        // sort by reverse rsquared, or negative r-squared... note the `-` in `eq -> - eq.getRSquared()`
        return Arrays.stream(models).map(m -> detect(x, y, m)).sorted(ranking).toArray(size -> new MathEquation[size]);
    }

    static MathEquation detect(double[] x, double[] y, MathModel model) {
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
        MathEquation eq = new MathEquation(model, finalFunction, results, model.getFormat(), rSquared);
        return eq;
    }

    private static double calculateRSquared(DoubleUnaryOperator finalFunction, double[] x, double[] y) {
        double yAverage = Arrays.stream(y).average().getAsDouble();
        double variance = 0;
        double residualSumOfSquares = 0;
//        double explainedSumOfSquares = 0;

        for (int i = 0; i < y.length; i++) {
            double yi = y[i];
            double fi = finalFunction.applyAsDouble(x[i]);
            variance += (yi - yAverage) * (yi - yAverage);
            residualSumOfSquares += (yi - fi) * (yi - fi);
//            explainedSumOfSquares += (fi - yAverage) * (fi - yAverage);
        }

        double rSquared = 1 - residualSumOfSquares / variance;
        return rSquared;
    }

}
