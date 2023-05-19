package MyFirstModel;

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
    public double requestCouponPayments(double time, double currentCPI) {
        // current CPI is unused in this function but it makes it easier to have it there - fix maybe later idk
        if (time == endTime) {
            return faceValue + rate * faceValue;
        } else if (time < endTime) {
            return rate * faceValue;
        } else {
            return 0.0; // TODO: this is kinda inefficient - do this check elsewhere
        }
    }

    @Override
    public double calculateDuration(double currentTime, double currentInterestRate, double currentCPI) {
        int length = (int) Math.round(endTime - currentTime);
        return (1 + currentInterestRate) / currentInterestRate - (1 + currentInterestRate + length * (rate - currentInterestRate)) / (rate * (Math.pow(1 + currentInterestRate, length - 1)) + currentInterestRate);
    }
    // make an interface which is bond or something and make it have an evaluate fn
    // and then loop over it and call value on everything in the list so thats easy
    // except it might not be easy to value the index bond
    // so in the beginning we let the traders value the bonds by assuming rpi is about 5% and then let them deal with the consequences
//    public double calculateDuration(double time) {
//        // TODO: sort out payments happening twice a year
//        double numerator = 0.0;
//        for (int i = 0; i < (endTime - time); i++) {
//            numerator =
//        }
//        return 1/2 *
//    }
}
