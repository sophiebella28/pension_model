package MyFirstModel;

import org.apache.commons.io.IOUtils;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.abm.Section;
import simudyne.core.annotations.Variable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

public class PensionFund extends Agent<MyModel.Globals> {
    @Variable
    public double cashVal;
    private double currentInterestRate;
    private double currentInflationRate;

    @Variable
    private double currentLiabilityVal;

    private double currentDuration;

    @Variable
    private double currentValue;
    private List<Bond> portfolio;
    private final List<Liability> liabilities;
    public PensionFund() {
        portfolio = new ArrayList<Bond>();
        liabilities = new ArrayList<>(Arrays.asList(new Liability(3000, 30.0)));
    }

    private double valuePortfolioAtTime(double futureTime, double currentTime) {
        double totalValue = 0.0;
        for (double i = currentTime; i <= futureTime; i++) { // TODO this will be annoying
            for (Bond bond : portfolio) {
                totalValue += bond.requestCouponPayments(i, currentInflationRate);
            }
        }
        return totalValue;
    }

    private void calculatePortfolioValueAndDuration(double currentTime) {
        double totalValue = 0.0;
        double totalDuration = 0.0;
        for (Bond bond : portfolio) {
            System.out.println("in bond loop" + currentInterestRate + " inflation " + currentInflationRate);
            double bondVal = bond.valueBond(currentInterestRate, currentTime, currentInflationRate);
            System.out.println("bond val" + bondVal);
            totalValue += bondVal;
            totalDuration += bondVal * bond.calculateDuration(currentTime, currentInterestRate, currentInflationRate);
        }
        if (portfolio.size() > 0) {
            currentValue = totalValue;
            currentDuration = totalDuration / totalValue;
        } else {
            currentValue = 0.0;
            currentDuration = 0.0;
        }

        System.out.println("current value is" + currentValue);

    }


    private double totalLiabilitiesAtTime(double time) {
        return liabilities.stream().filter(liability -> liability.dueDate == time)
                .map(liability -> liability.amount).mapToDouble(Double::doubleValue).sum();
    }



    public static Action<PensionFund> payLiabilities(double time) {
      return Action.create(PensionFund.class, pensionFund -> {
          double totalLiabilities = pensionFund.totalLiabilitiesAtTime(time);
          System.out.println();
              ListIterator<Bond> iterator = pensionFund.portfolio.listIterator();
              // If the pension fund can't pay the liability, sell bonds until either it can pay or there are no more bonds
              while (pensionFund.cashVal < totalLiabilities && iterator.hasNext()) {
                  Bond bond = iterator.next();
                  double bondVal = bond.valueBond(pensionFund.currentInterestRate, time, pensionFund.currentInflationRate);
                  pensionFund.cashVal += bondVal;
                  pensionFund.getLinks(Links.MarketLink.class).send(Messages.SellBonds.class, (msg, link) ->
                  {
                      msg.bondToSell = bond;
                      msg.bondVal = bondVal;
                  });
                  iterator.remove();
              }
              pensionFund.cashVal -= totalLiabilities;
              pensionFund.liabilities.removeIf(liability -> liability.dueDate <= time);

            });
    }
    public static Action<PensionFund> receiveCoupons(double time) {
        return Action.create(PensionFund.class, pensionFund -> {
            double totalCoupons = pensionFund.getMessagesOfType(Messages.CouponPayment.class).stream()
                    .map(coupon -> coupon.coupons).findFirst().orElse(0.0);
            pensionFund.cashVal += totalCoupons;
            pensionFund.portfolio.removeIf(bond -> bond.getEndTime() <= time);
        });
    }

    public static Action<PensionFund> receiveInterestRates() {
        return Action.create(PensionFund.class, pensionFund -> {
            double[] rates = pensionFund.getMessagesOfType(Messages.InterestUpdate.class).stream()
                    .map(request -> request.rates).findFirst().orElse(new double[] {0.0, 0.0});
            pensionFund.currentInterestRate = rates[0];
            pensionFund.currentInflationRate = rates[1]; // extract the rates out of the message object
            pensionFund.liabilities.forEach(liability -> liability.updateLiabilityAmount(pensionFund.currentInflationRate)); // TODO check this works
            pensionFund.currentLiabilityVal = pensionFund.liabilities.stream().map(liability -> liability.amount).reduce(Double::sum).orElse(0.0);
        });
    }

    public static Action<PensionFund> buyHedges(double time, double timestep) {
        return Action.create(PensionFund.class, pensionFund -> {
            pensionFund.liabilities.forEach(
                    // pension fund spends an amount equivalent to the (predicted) liability on interest bonds
                    // the bond issuer needs to be the one to decide the coupons of these bonds and the face value etc
                    // so bond issuer will need to send the bonds back to us
                    // actually pension fund might not need to have the bonds stored?

                    // timestep 1: pension fund spends an amount equivalent to the predicted liability on interest bonds
                    // this amount may or may not assume reinvestment - i guess it needs to bc otherwise it reduces into
                    // a base case
                    // after timestep 1 they calculate how much money they now think the liability will be
                    // and they compare this to the amount of money that they will have reinvesting at the current rate
                    // THEN they reinvest the necessary coupons - so each liability will basically need to have its own
                    // portfolio of bonds
                    // can this be abstracted away into weights and stuff?????????????????????????? maybe
                    // anyway we move - we reinvest the necessary coupons
                    // and calculate the new expected value of the portfolio at end time
                    // and THEN we WAIT NO WE DONT NEED TO REINVEST COUPONS YET - we calculate the difference and reinvest
                    // however much needed to make up that difference
                    liability -> {
                        //TODO THIS WONT WORK AS SOON!!!!! AS SOON AS I HAVE MORE THAN ONE LIABILITY
                        // 1. work out how much money the liability will be - assume cpi ratio now is the same as the future
                        double liabilityFutureValue = liability.valueLiability(pensionFund.currentInflationRate, time);
                        // 2. work out value of portfolio if all coupons reinvested at current rate - reinvestment is really confusing, just work out how much cash
                        // I can't figure out how reinvestment works for the life of me so I am not bothering with it, this just works out how much money we will have in the future
                        // With the bonds that we currently own
                        double portfolioFutureValue = pensionFund.valuePortfolioAtTime(liability.dueDate, time) + pensionFund.cashVal;
                        // 3. take difference
                        double requiredFunds = Math.max(liabilityFutureValue - portfolioFutureValue, 0.0); // if we will have more money than needed we just keep the bond so we can have more money
                        // 4. buy bonds that will give us the difference
                        pensionFund.calculatePortfolioValueAndDuration(time);
                        System.out.println(pensionFund.portfolio.stream().map(Bond::getEndTime).collect(Collectors.toList()));
                        double length = liability.dueDate - time;
                        System.out.println("/home/sophie/Documents/uni/project/git_folder/bash/activate_run_python.sh" + " " + Double.toString(pensionFund.currentInterestRate) + " " +
                                Double.toString(pensionFund.currentInflationRate) + " " + Double.toString(pensionFund.currentDuration) + " " + Double.toString(length) + " " + Double.toString(pensionFund.currentValue + pensionFund.cashVal) + " " + Double.toString(liability.amount));

                        ProcessBuilder processBuilder = new ProcessBuilder("/home/sophie/Documents/uni/project/git_folder/bash/activate_run_python.sh", Double.toString(pensionFund.currentInterestRate),
                                Double.toString(pensionFund.currentInflationRate), Double.toString(pensionFund.currentDuration), Double.toString(length), Double.toString(pensionFund.currentValue + pensionFund.cashVal), Double.toString(liability.amount));
                        processBuilder.redirectErrorStream(true);
                        Process process = null;
                        String results;
                        try {
                            process = processBuilder.start();
                            results = IOUtils.toString(process.getInputStream(), "UTF-8");
                            System.out.println(results);
                        } catch (IOException e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                        try {
                            int exitCode = process.waitFor();
                            System.out.println("exit code" + exitCode);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        String[] resultsArray = results.split(",");
                        double requiredLength = Double.parseDouble(resultsArray[0]);
                        double requiredAmount = Double.parseDouble(resultsArray[1]);
                        // This calculates the amount of bonds we need to buy now with current interest rates
                        if (requiredAmount > 1e-06) { // adds a small amount of tolerance
                            System.out.println("required Length: " + requiredLength);
                            System.out.println((int) Math.round(requiredLength));
                            Bond newBond = new InterestBond(time + Math.round(requiredLength), pensionFund.currentInterestRate, requiredAmount);
                            pensionFund.getLinks(Links.MarketLink.class).send(Messages.PurchaseBonds.class, (msg, link) -> {
                                msg.bondToPurchase = newBond;
                            });
                            pensionFund.portfolio.add(newBond);
                        } // TODO: I also need to make sure that the fund sells all their bonds when they need to meet the liability
                    }
            );
        });
    }
}

