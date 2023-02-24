package MyFirstModel;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

import java.util.List;
import java.util.Random;

public class BondIssuer extends Agent<MyModel.Globals> {

    @Variable
    public double interestRate;

    @Variable
    public double moneyPaid;

    private Random random;

    public BondIssuer() {
        interestRate = 0.02;
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
                bondIssuer.updateInterestRate(theta);
                bondIssuer.getLinks(Links.MarketLink.class).send(Messages.InterestUpdate.class, (msg, link) -> msg.currentRate = bondIssuer.interestRate);
            });}
    void updateInterestRate(double theta) {
        interestRate += (theta - getGlobals().drift * interestRate) * interestRate + getGlobals().volatility * random.nextGaussian();
    }


}
