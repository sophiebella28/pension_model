package MyFirstModel;

public class InterestBond {
    
    private double finalCoupon;
    private double endTick;
    private double rate;

    public double getValueAtNextTimestep(double currentValue) {
        return (1.05) * currentValue;
    }

    public double requestCouponPayments(double time) {
        if (time == endTick) {
            return finalCoupon;
        } else {
            return rate;
        }
    }

    // make an interface which is bond or something and make it have an evaluate fn
    // and then loop over it and call value on everything in the list so thats easy
    // except it might not be easy to value the index bond
    // so in the beginning we let the traders value the bonds by assuming rpi is about 5% and then let them deal with the consequences

}
