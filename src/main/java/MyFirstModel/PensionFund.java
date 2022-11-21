package MyFirstModel;

import scala.Int;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

import java.util.List;

public class PensionFund extends Agent<MyModel.Globals> {
    @Variable
    public double cashVal;

    @Variable
    public double liabilities;

    @Variable
    public double fixedBondsVal;

    @Variable
    public double indexBondsVal;

    private List<InterestBond> interestBonds;



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


}
