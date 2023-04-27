package MyFirstModel;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
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
    private MultivariateNormalDistribution mvn;

    public BondIssuer() {
        longRate = 0.02;
        inflationRate = 0.02;
        interestRate = 0.02;
        shortRate = 0.02;
        random = new Random();
        mvn = new MultivariateNormalDistribution(new double[]{0.0, 0.0}, new double[][] {{1.0,0.19},{0.19,1.0}});
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
                bondIssuer.getLinks(Links.MarketLink.class).send(Messages.InterestUpdate.class,
                        (msg, link) -> {
                    msg.rates = new double[] {bondIssuer.interestRate, bondIssuer.inflationRate};
                });
            });}
    void updateRates(double theta) {
        double[] randomVals = mvn.sample();
        System.out.println("first" + (theta - getGlobals().driftShortTerm * interestRate));
        System.out.println("second" + getGlobals().volatilityShortTerm * randomVals[0]);
        interestRate += (theta - getGlobals().driftShortTerm * interestRate) * interestRate + getGlobals().volatilityShortTerm * randomVals[0];
        inflationRate =  1.0179 * inflationRate + randomVals[1] + 0.0366;
        System.out.println("Interest Rate Next" + interestRate);
    }


}
