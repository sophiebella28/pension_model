package MyFirstModel;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import simudyne.core.ModelContext;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.rng.SeededRandom;

import java.util.*;

public class BondIssuer extends Agent<MyModel.Globals> {

    // @Variable(initializable = true)
    public double interestRate = 0.02;

    // @Variable(initializable = true)
    public double inflationRate = 0.02;
    @Variable(initializable = true)
    public double totalMoney = 0.0;

    private MultivariateNormalDistribution mvn;
    private Map<Long, List<Bond>> portfolios;
    public BondIssuer() {
        portfolios = new HashMap<>();
    }

    public static Action<BondIssuer> receiveHedges() {
        return Action.create(BondIssuer.class, bondIssuer -> {
            bondIssuer.getMessagesOfType(Messages.PurchaseBonds.class).forEach(purchaseBonds ->
            {
                if (bondIssuer.portfolios.containsKey(purchaseBonds.getSender())) {
                    bondIssuer.portfolios.get(purchaseBonds.getSender()).add(purchaseBonds.bondToPurchase);
                } else {
                    bondIssuer.portfolios.put(purchaseBonds.getSender(), new ArrayList<>(Collections.singletonList(purchaseBonds.bondToPurchase)));
                }
                System.out.println("keyset:" + bondIssuer.portfolios.keySet());
                bondIssuer.totalMoney += purchaseBonds.bondToPurchase.getFaceValue();
            });
        });
    }

    public static Action<BondIssuer> receiveSoldBonds() {
        return Action.create(BondIssuer.class, bondIssuer -> {
            bondIssuer.getMessagesOfType(Messages.SellBonds.class).forEach(sellBonds ->
            {
                if (bondIssuer.portfolios.containsKey(sellBonds.getSender())) {
                    bondIssuer.portfolios.get(sellBonds.getSender()).remove(sellBonds.bondToSell);
                } else {
                    throw new RuntimeException("Selling bond owned by pension fund that doesn't exist");
                }
                bondIssuer.totalMoney -= sellBonds.bondVal;
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
                for (long pensionFundID : bondIssuer.portfolios.keySet()) { // Iterate over all bonds and total up amount of coupons to send
                    double totalCoupons = 0.0;
                    for (Bond bond : bondIssuer.portfolios.get(pensionFundID)){
                        totalCoupons += bond.requestCouponPayments(time, bondIssuer.inflationRate);
                    }
                    double finalTotalCoupons = totalCoupons;
                    bondIssuer.send(Messages.CouponPayment.class, (msg) -> msg.coupons = finalTotalCoupons).to(pensionFundID);
                    bondIssuer.totalMoney -= finalTotalCoupons;
                    bondIssuer.portfolios.get(pensionFundID).removeIf(bond -> bond.getEndTime() <= time);
                }
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
        mvn = getPrng().multivariateNormal(new double[]{0.0, 0.0}, new double[][]{{1.0, 0.19}, {0.19, 1.0}}); // TODO this may no longer be random
        double[] randomVals = mvn.sample();
        interestRate += ( getGlobals().driftShortTerm ) * interestRate + getGlobals().volatilityShortTerm * randomVals[0];
        inflationRate = 0.000383 + 0.982335 * inflationRate + Math.pow(0.03, 2.0) * randomVals[1]; //TODO: remove magic numbers
    }


}
