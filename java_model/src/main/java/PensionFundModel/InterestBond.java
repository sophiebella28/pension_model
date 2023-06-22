package PensionFundModel;

public class InterestBond implements Bond{

    private double endTime;
    private double rate;
    private double faceValue;

    public InterestBond(double endTime, double rate, double faceValue) {
        this.endTime = endTime;
        this.rate = rate;
        this.faceValue = faceValue;
    }

    @Override
    public double valueBond(double currentRate, double currentTime, double currentCPI) {
        int length = (int) Math.round(endTime - currentTime);
        double price = 0.0;
        for (int i = 1; i < length; i++) {
            price += (rate * faceValue) / Math.pow(1 + currentRate, i);
        }
        price += (rate * faceValue + faceValue) / Math.pow(1 + currentRate, length);
        return price;
    }

    @Override
    public double getEndTime() {
        return endTime;
    }

    @Override
    public double getFaceValue() { return faceValue; }
    @Override
    public double requestCouponPayments(double time, double currentCPI) {
        if (time >= endTime) {
            return faceValue + rate * faceValue;
        } else {
            return rate * faceValue;
        }
    }

    @Override
    public double calculateDuration(double currentTime, double currentInterestRate, double currentCPI) {
        int length = (int) Math.round(endTime - currentTime);
        return (1 + currentInterestRate) / currentInterestRate -
                (1 + currentInterestRate + length * (rate - currentInterestRate)) / (rate * (Math.pow(1 + currentInterestRate, length) - 1) + currentInterestRate);
    }

}
