package MyFirstModel;

public class InterestBond {

    private double endTime;
    private double rate;
    private double faceValue;

    public InterestBond(double endTime, double rate, double faceValue) {
        this.endTime = endTime;
        this.rate = rate;
        this.faceValue = faceValue;
    }

    public double getValueAtNextTimestep(double time) {
        return (1.05) * faceValue; // this is useless rn
    }

    public double requestCouponPayments(double time) {
        if (time == endTime) {
            return faceValue + rate * faceValue;
        } else if (time < endTime) {
            return rate * faceValue;
        } else {
            return 0.0; // TODO: this is kinda inefficient
        }
    }

    // make an interface which is bond or something and make it have an evaluate fn
    // and then loop over it and call value on everything in the list so thats easy
    // except it might not be easy to value the index bond
    // so in the beginning we let the traders value the bonds by assuming rpi is about 5% and then let them deal with the consequences

}
