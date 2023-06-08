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
        accumulatedCPI *= currentCPI;
        if (time >= endTime) {
            return couponRate * faceValue + faceValue;
        } else {
            return couponRate * faceValue;
        }
    }

    public double valueBond(double currentRate, double currentTime, double currentCPI) {
        int length = (int) Math.round(endTime - currentTime);
        double price = 0.0;
        for (int i = 1; i < length; i++) {
            price += (couponRate * accumulatedCPI * currentCPI * faceValue) / Math.pow(1 + currentRate, i);
        }
        price += (couponRate * accumulatedCPI * currentCPI  * faceValue + faceValue) / Math.pow(1 + currentRate, length);
        return price;
    }

    @Override
    public double calculateDuration(double currentTime, double currentInterestRate, double currentCPI) {
        int length = (int) Math.round(endTime - currentTime);
        double realRedemptionYield = (1 + currentInterestRate) * accumulatedCPI * currentCPI;
        double numerator = 0;
        double denominator = 0;
        for (int i = 1; i < length; i++) {
            double ithContribution = couponRate * faceValue * Math.pow(1/realRedemptionYield, i);
            numerator += ithContribution * i;
            denominator += ithContribution;
        }
        numerator += (couponRate + 1) * faceValue * Math.pow(1/realRedemptionYield, length) * length;
        denominator += (couponRate + 1) * faceValue * Math.pow(1/realRedemptionYield, length);
        return numerator/denominator;
    }
}
