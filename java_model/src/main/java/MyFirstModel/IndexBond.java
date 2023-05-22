package MyFirstModel;

public class IndexBond implements Bond {


    private final double endTime;
    private final double couponRate;
    private final double initialCPI;
    private final double faceValue;

    public IndexBond(double endTime, double couponRate, double initialCPI, double faceValue) {
        this.endTime = endTime;
        this.couponRate = couponRate;
        this.initialCPI = initialCPI;
        this.faceValue = faceValue;
    }

    @Override
    public double getEndTime() {
        return endTime;
    }

    @Override
    public double requestCouponPayments(double time, double currentCPI) {
        if (time >= endTime) {
            return couponRate * currentCPI / initialCPI * faceValue + faceValue;
        } else {
            return couponRate * currentCPI / initialCPI * faceValue;
        }
    }

    public double valueBond(double currentRate, double currentTime, double currentCPI) {
        int length = (int) Math.round(endTime - currentTime);
        double price = 0.0;
        for (int i = 1; i < length; i++) {
            price += (couponRate * (currentCPI / initialCPI) * faceValue) / Math.pow(1 + currentRate, i);
        }
        price += (couponRate * (currentCPI / initialCPI) * faceValue + faceValue) / Math.pow(1 + currentRate, length);
        return price;
    }

    @Override
    public double calculateDuration(double currentTime, double currentInterestRate, double currentCPI) {
        int length = (int) Math.round(endTime - currentTime);
        double realRedemptionYield = (1 + currentInterestRate) * (currentCPI / initialCPI);
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
