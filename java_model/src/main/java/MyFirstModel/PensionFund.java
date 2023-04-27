package MyFirstModel;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

import java.util.ArrayList;
import java.util.List;

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


    private double currentInterestRate;
    private double currentInflationRate;

    private final List<InterestBond> interestBonds;
    private final List<IndexBond> indexBonds;

    public PensionFund() {
        interestBonds = new ArrayList<InterestBond>();
        indexBonds = new ArrayList<IndexBond>();
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
            for (IndexBond bond : pensionFund.indexBonds) {
                totalCoupons += bond.requestCouponPayments(time, pensionFund.currentInflationRate);
            }
            final double finalTotalCoupons = totalCoupons;
            pensionFund.cashVal += totalCoupons;
            pensionFund.getLinks(Links.MarketLink.class).send(Messages.CouponRequest.class, (msg, link) -> msg.coupons = finalTotalCoupons);

        });
    }

    public static Action<PensionFund> receiveInterestRates(double time, double timestep) {
           return Action.create(PensionFund.class, pensionFund -> {
                double[] rates = pensionFund.getMessagesOfType(Messages.InterestUpdate.class).stream()
                        .map(request -> request.rates).findFirst().orElse(new double[] {0.0, 0.0});
                pensionFund.currentInterestRate = rates[0];
                pensionFund.currentInflationRate = rates[1]; // extract the rates out of the message object

                double moneyNeeded = pensionFund.liabilities - pensionFund.valuePortfolioAtNextTimestep();
                double totalBondsToPurchase = 20 * moneyNeeded; // As we assume a 5% interest rate
               System.out.println("current rate:" + pensionFund.currentInterestRate);
                // TODO: make this much more complex
               if (totalBondsToPurchase > 0.0) {
                InterestBond newInterestBond = new InterestBond(time + 13 * timestep, pensionFund.currentInterestRate, totalBondsToPurchase / 2);
                pensionFund.interestBonds.add(newInterestBond);
                IndexBond newIndexBond = new IndexBond(time + 13 * timestep, pensionFund.currentInterestRate, pensionFund.currentInflationRate, totalBondsToPurchase/ 2);
                pensionFund.indexBonds.add(newIndexBond);
               }
            }); }

    private double valuePortfolioAtNextTimestep() {
        double time = getGlobals().time;
        double totalVal = cashVal;
        for (InterestBond bond : interestBonds) {
            totalVal += bond.requestCouponPayments(time + getGlobals().timeStep);
        }
        for (IndexBond bond : indexBonds) {
            totalVal += bond.requestCouponPayments(time + getGlobals().timeStep, currentInflationRate);
        }
        System.out.println("Value next timestep " + totalVal);
        return totalVal;
    }
}
