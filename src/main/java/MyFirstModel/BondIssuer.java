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

    public static Action<BondIssuer> giveCoupons =
            Action.create(BondIssuer.class, bondIssuer -> {
                double totalCouponsToPay = bondIssuer.getMessagesOfType(Messages.CouponRequest.class).stream()
                        .map(request -> request.coupons).reduce(0.0, Double::sum);
                bondIssuer.moneyPaid += totalCouponsToPay;
            });

    public static Action<BondIssuer> updateInterest =
            Action.create(BondIssuer.class, bondIssuer -> {
                bondIssuer.updateInterestRate();
                bondIssuer.getLinks(Links.MarketLink.class).send(Messages.InterestUpdate.class, (msg, link) -> msg.currentRate = bondIssuer.interestRate);
            });
    void updateInterestRate() {
        interestRate += getGlobals().drift * getGlobals().timeStep + getGlobals().volatility * new Random().nextGaussian() * Math.sqrt(getGlobals().timeStep);
    }


}
