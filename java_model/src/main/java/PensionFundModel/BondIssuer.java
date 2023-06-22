package PensionFundModel;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

import java.util.*;

public class BondIssuer extends Agent<MyModel.Globals> {

    @Variable
    public double interestRate;

    @Variable
    public double inflationRate;
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
                    msg.interestRate = bondIssuer.interestRate;
                    msg.inflationRate = bondIssuer.inflationRate; }
                );
            });}
    void updateRates(double theta) {
        mvn = getPrng().multivariateNormal(new double[]{0.0, 0.0}, new double[][]{{1.0, getGlobals().corr}, {getGlobals().corr, 1.0}});
        double[] randomVals = mvn.sample();
        interestRate += ( ( theta - getGlobals().interestDrift * interestRate)  + getGlobals().interestVolatility * randomVals[0]);
        inflationRate = getGlobals().inflationConstant + getGlobals().inflationDrift * inflationRate + getGlobals().inflationVolatility * randomVals[1];
        if (inflationRate < 0.0) {
            inflationRate += 0.01;
        }
    }


}
