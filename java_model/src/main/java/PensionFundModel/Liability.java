package PensionFundModel;

public class Liability {
    public double amount;
    public final double dueDate;


    public Liability(double amount, double dueDate, double initCPI) {
        this.amount = amount; // NOTE: this does not remain constant - it changes depending on the RPI changes
        this.dueDate = dueDate;
    }

    public void updateLiabilityAmount(double currentCPI) {
        amount = amount * Math.max(Math.min(currentCPI + 1, 1.05), 1.0);
    }

    public double valueLiability(double currentCPI, double currentInterestRate, double currentTime) {
        int length = (int) Math.round(dueDate - currentTime);
        return amount * Math.pow(Math.max(Math.min(currentCPI + 1, 1.05), 1.0)/ (1 + currentInterestRate), length);
    }
}
