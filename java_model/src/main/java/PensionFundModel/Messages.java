package PensionFundModel;

import simudyne.core.graph.Message;

public class Messages {
    public static class CouponPayment extends Message {
        public double coupons;
    }

    public static class InterestUpdate extends Message {
        public double interestRate;
        public double inflationRate;
    }

    public static class PurchaseBonds extends Message {
        public Bond bondToPurchase;
    }

    public static class SellBonds extends Message {
        public Bond bondToSell;
        public double bondVal;
    }
}
