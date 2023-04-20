package MyFirstModel;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

import java.util.Random;

public class BondIssuer extends Agent<MyModel.Globals> {

    @Variable
    public double longRate;

    @Variable
    public double shortRate;

    @Variable
    public double interestRate;

    @Variable
    public double inflationRate;
    @Variable
    public double moneyPaid;

    private Random random;

    public BondIssuer() {
        longRate = 0.02;
        inflationRate = 0.02;
        interestRate = 0.02;
        shortRate = 0.02;
        random = new Random();
    }

    public static Action<BondIssuer> giveCoupons =
            Action.create(BondIssuer.class, bondIssuer -> {
                double totalCouponsToPay = bondIssuer.getMessagesOfType(Messages.CouponRequest.class).stream()
                        .map(request -> request.coupons).reduce(0.0, Double::sum);
                bondIssuer.moneyPaid = totalCouponsToPay;
            });

    public static Action<BondIssuer> updateInterest(double theta) {
            return Action.create(BondIssuer.class, bondIssuer -> {
                bondIssuer.updateRates(theta);
                bondIssuer.getLinks(Links.MarketLink.class).send(Messages.InterestUpdate.class, (msg, link) -> msg.currentRate = bondIssuer.interestRate);
            });}
    void updateRates(double theta) {
        // TODO: put thetas back in, recalibrate with my own parameters instead of ones from the 1900s
        inflationRate += getGlobals().driftInflation * (getGlobals().muInflation - inflationRate) + getGlobals().volatilityInflation * random.nextGaussian();
        shortRate += getGlobals().driftShortTerm * (longRate - shortRate) + getGlobals().volatilityShortTerm * random.nextGaussian();
        longRate += getGlobals().driftLongTerm * (getGlobals().muShortTerm - longRate)  + getGlobals().volatilityLongTerm * random.nextGaussian();
        interestRate += shortRate * inflationRate;
    }


}
