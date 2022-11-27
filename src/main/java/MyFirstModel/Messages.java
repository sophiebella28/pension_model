package MyFirstModel;

import simudyne.core.graph.Message;

public class Messages {
    public static class CouponRequest extends Message {
        public double coupons;
    }

    public static class InterestUpdate extends Message {
        public double currentRate;
    }
}
