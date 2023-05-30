package PensionFundModel;

public interface Bond {
    double valueBond(double currentRate, double currentTime, double currentCPI);

    double getEndTime();

    double getFaceValue();

    double requestCouponPayments(double time, double scale);

    double calculateDuration(double currentTime, double currentInterestRate, double currentCPI);
}
