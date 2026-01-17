package org.example.metrics;

public class ReliabilityMetrics {
    private final int bothPositive;
    private final int rater1PositiveRater2Negative;
    private final int rater1NegativeRater2Positive;
    private final int bothNegative;
    private final double observedAgreement;
    private final double kappa;
    private final double ac1;

    public ReliabilityMetrics(
            int bothPositive,
            int rater1PositiveRater2Negative,
            int rater1NegativeRater2Positive,
            int bothNegative,
            double observedAgreement,
            double kappa,
            double ac1
    ) {
        this.bothPositive = bothPositive;
        this.rater1PositiveRater2Negative = rater1PositiveRater2Negative;
        this.rater1NegativeRater2Positive = rater1NegativeRater2Positive;
        this.bothNegative = bothNegative;
        this.observedAgreement = observedAgreement;
        this.kappa = kappa;
        this.ac1 = ac1;
    }

    public int getBothPositive() {
        return bothPositive;
    }

    public int getRater1PositiveRater2Negative() {
        return rater1PositiveRater2Negative;
    }

    public int getRater1NegativeRater2Positive() {
        return rater1NegativeRater2Positive;
    }

    public int getBothNegative() {
        return bothNegative;
    }

    public double getObservedAgreement() {
        return observedAgreement;
    }

    public double getKappa() {
        return kappa;
    }

    public double getAc1() {
        return ac1;
    }
}
