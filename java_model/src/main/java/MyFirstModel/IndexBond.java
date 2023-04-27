package MyFirstModel;

public class IndexBond {


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

    public double requestCouponPayments(double time, double currentCPI) {
        if (time == endTime) {
            return couponRate * currentCPI / initialCPI * faceValue;
        } else if (time < endTime) {
            return couponRate * currentCPI / initialCPI * faceValue;
        } else {
            return 0.0; // TODO: this is kinda inefficient
        }
    }
}
