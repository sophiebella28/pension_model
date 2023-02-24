package MyFirstModel;

import scala.Int;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PensionFund extends Agent<MyModel.Globals> {
    @Variable
    public double cashVal;

    @Variable
    public double liabilities;
    // Currently we assume that this never changes - the pension scheme needs to pay the same amount of money every tick forever

//    @Variable
//    public double fixedBondsVal;
//
//    @Variable
//    public double indexBondsVal;

    private double currentRate;

    private final List<InterestBond> interestBonds;

    public PensionFund() {
        interestBonds = new ArrayList<InterestBond>();
        liabilities = 100;
    }

    public static Action<PensionFund> payLiabilities =
            Action.create(PensionFund.class, pensionFund -> {
                pensionFund.cashVal -= pensionFund.liabilities;
            });

    public static Action<PensionFund> requestCoupons(double time) {
        return Action.create(PensionFund.class, pensionFund -> {
            double totalCoupons = 0.0;
            for (InterestBond bond : pensionFund.interestBonds) {
                totalCoupons += bond.requestCouponPayments(time);
            }
            final double finalTotalCoupons = totalCoupons;
            pensionFund.cashVal += totalCoupons;
            pensionFund.getLinks(Links.MarketLink.class).send(Messages.CouponRequest.class, (msg, link) -> msg.coupons = finalTotalCoupons);

        });
    }

    public static Action<PensionFund> receiveInterestRates(double time, double timestep) {
           return Action.create(PensionFund.class, pensionFund -> {
                pensionFund.currentRate = pensionFund.getMessagesOfType(Messages.InterestUpdate.class).stream()
                        .map(request -> request.currentRate).findFirst().orElse(0.0);
                double moneyNeeded = pensionFund.liabilities - pensionFund.valuePortfolioAtNextTimestep();
                double totalBondsToPurchase = 20 * moneyNeeded; // As we assume a 5% interest rate
               System.out.println("current rate:" + pensionFund.currentRate);
                // TODO: make this much more complex
               if (totalBondsToPurchase > 0.0) {
                InterestBond newBond = new InterestBond(time + 13 * timestep, pensionFund.currentRate, totalBondsToPurchase);
                pensionFund.interestBonds.add(newBond);
               }
            }); }

    private double valuePortfolioAtNextTimestep() {
        double time = getGlobals().time;
        double totalVal = cashVal;
        for (InterestBond bond : interestBonds) {
            totalVal += bond.requestCouponPayments(time + getGlobals().timeStep);
        }
        System.out.println("Value next timestep " + totalVal);
        return totalVal;
    }
}
