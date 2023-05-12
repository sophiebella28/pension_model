package MyFirstModel;

public class Liability {
    public double amount;
    public final double dueDate;


    public Liability(double amount, double dueDate) {
        this.amount = amount; // NOTE: this isn't fixed - it depends on the RPI changes
        this.dueDate = dueDate;
    }

    public void updateLiabilityAmount(double currentCPI) {
        amount = amount * Math.min(1.0 + currentCPI, 1.05);
    }

    public double valueLiability(double forecastedCPI, double time) {
        System.out.println("amount " + amount);
        System.out.println("power " + Math.min(1.0 + forecastedCPI, 1.05));
        System.out.println("forecastedCPI " + forecastedCPI);

        return amount * Math.pow(Math.min(1.0 + forecastedCPI, 1.05), dueDate - time);

    }
}
