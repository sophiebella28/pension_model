package PensionFundModel;

public class IndexBond implements Bond {


    private final double endTime;
    private final double couponRate;
    private final double faceValue;
    private double accumulatedCPI;
    public IndexBond(double endTime, double couponRate, double faceValue) {
        this.endTime = endTime;
        this.couponRate = couponRate;
        this.faceValue = faceValue;
        accumulatedCPI = 1.0;
    }

    public IndexBond(double endTime, double currentInterestRate, double inflationPrediction, double currentValue, double time) {
        this.endTime = endTime;
        this.couponRate = currentInterestRate * 0.5;
        this.faceValue = calculateFaceValue(currentInterestRate, inflationPrediction, endTime - time, currentValue);
        accumulatedCPI = 1.0;
    }

    private double calculateFaceValue(double currentInterestRate, double inflationPrediction, double length, double currentValue) {
        double powerTerm = Math.pow(inflationPrediction, length) / Math.pow(1 + currentInterestRate, length);
        double sumTerm = couponRate * inflationPrediction * ( 1 - powerTerm) / (1 + currentInterestRate - inflationPrediction);
        return currentValue / (powerTerm + sumTerm);
    }

    @Override
    public double getEndTime() {
        return endTime;
    }

    @Override
    public double getFaceValue() {
        return faceValue;
    }

    @Override
    public double requestCouponPayments(double time, double currentCPI) {
        accumulatedCPI *= (1 + currentCPI);
        if (time >= endTime) {
            return couponRate * faceValue * accumulatedCPI + faceValue * accumulatedCPI;
        } else {
            return couponRate * faceValue * accumulatedCPI;
        }
    }

    public double valueBond(double currentRate, double currentTime, double currentCPI) {
        int length = (int) Math.round(endTime - currentTime);
        double price = 0.0;
        for (int i = 1; i < length; i++) {
            price += (couponRate * accumulatedCPI * Math.pow(currentCPI + 1, i) * faceValue) / Math.pow((1 + currentRate), i);
        }
        price += ((couponRate + 1) * accumulatedCPI * Math.pow(currentCPI + 1, length) * faceValue) /  Math.pow((1 + currentRate), length);
        return price;
    }

    @Override
    public double calculateDuration(double currentTime, double currentInterestRate, double currentCPI) {
        int length = (int) Math.round(endTime - currentTime);
        double yield = ( currentInterestRate - currentCPI ) / (1 + currentCPI);
        return (1 + yield) / yield -
                (1 + yield + length * (couponRate - yield)) / (couponRate * (Math.pow(1 + yield, length) - 1) + yield);
    }
}
