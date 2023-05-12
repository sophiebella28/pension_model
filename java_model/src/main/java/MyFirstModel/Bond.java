package MyFirstModel;

public interface Bond {
    double getEndTime();
    double requestCouponPayments(double time, double scale);
}
