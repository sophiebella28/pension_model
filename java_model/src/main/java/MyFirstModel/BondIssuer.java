package MyFirstModel;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

import java.util.ArrayList;
import java.util.List;
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

    private List<Bond> bonds;
    public BondIssuer() {
        longRate = 0.02;
        inflationRate = 0.02;
        interestRate = 0.02;
        shortRate = 0.02;
        random = new Random();
        mvn = new MultivariateNormalDistribution(new double[]{0.0, 0.0}, new double[][] {{1.0,0.19},{0.19, 1.0}});
        bonds = new ArrayList<>();
    }

    public static Action<BondIssuer> receiveHedges() {
        return Action.create(BondIssuer.class, bondIssuer -> {
            bondIssuer.getMessagesOfType(Messages.PurchaseBonds.class).forEach(purchaseBonds ->
            {
                bondIssuer.bonds.add(purchaseBonds.bondToPurchase);
            });
        });
    }

    public static Action<BondIssuer> giveCoupons(double time) {
            // need to add something which iterates over all of the bonds in the portfolio and then calculates
            // the coupon payments to be paid for each bond in the portfolio and who to pay it to (can ignore for now
            // bc only one pension)

            // if i have a bond then i do the calculation in here?? but then how do I do the two different calculations?
            // can just do if x do this if y do this
            // I guess just have an interface that takes an additional scale value and set it to be 1 for fixed ones
            // but then it can be the same function for both?????? doesnt matter I think. This will prob help in the future
            return Action.create(BondIssuer.class, bondIssuer -> {
                double totalCoupons = 0.0;
                for (Bond bond : bondIssuer.bonds) { // Iterate over all bonds and total up amount of coupons to send
                    // If I have multiple pension funds then I will need to make my list of bonds into a map from each
                    // pension fund to a list of bonds and loop over pension funds then lists
                    // which I can do using messages I think so not too difficult
                    totalCoupons += bond.requestCouponPayments(time, bondIssuer.inflationRate);
                }
                double finalTotalCoupons = totalCoupons;
                bondIssuer.getLinks(Links.MarketLink.class).send(Messages.CouponPayment.class, (msg, link) -> msg.coupons = finalTotalCoupons);
                bondIssuer.moneyPaid = finalTotalCoupons;
                bondIssuer.bonds.removeIf(bond -> bond.getEndTime() <= time);
            });
    }
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
        interestRate += ( getGlobals().driftShortTerm ) * interestRate + getGlobals().volatilityShortTerm * randomVals[0];
        System.out.println(randomVals[1]);
        System.out.println("rate before " + inflationRate);
        inflationRate = 0.000383 + 0.982335 * inflationRate + Math.pow(0.03, 2.0) * randomVals[1]; //TODO: remove magic numbers
        System.out.println("rate after " + inflationRate);
    }


}
