package PensionFundModel;

public class Liability {
    public double amount;
    public final double dueDate;


    public Liability(double amount, double dueDate, double initCPI) {
        this.amount = amount; // NOTE: this isn't fixed - it depends on the RPI changes
        this.dueDate = dueDate;
    }

    public void updateLiabilityAmount(double currentCPI) {
        amount = amount * Math.max(Math.min(currentCPI + 1, 1.05), 1.0);
//        if(currentCPI + 1 > 1.05 || currentCPI + 1 < 1.0 ) {
//            System.out.println("CAP HIT");
//            System.out.println(currentCPI);
//        }
    }
}
