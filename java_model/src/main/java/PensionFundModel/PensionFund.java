package PensionFundModel;

import org.apache.commons.io.IOUtils;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PensionFund extends Agent<MyModel.Globals> {
    @Variable(initializable = true)
    public double cashVal = 3000.0;
    private double currentInterestRate;
    private double currentInflationRate;

    @Variable
    public double currentLiabilityVal;
    @Variable
    private double currentDuration;

    @Variable
    public double currentValue;
    private List<Bond> portfolio;
    private final List<Liability> liabilities;

    @Variable
    public String strategy;

    public PensionFund() {
        this.portfolio = new ArrayList<Bond>();
        this.liabilities = new ArrayList<>(Arrays.asList(new Liability(3000, 30.0)));
    }

    private void calculatePortfolioValueAndDuration(double currentTime) {
        double totalValue = 0.0;
        double totalDuration = 0.0;
        for (Bond bond : portfolio) {
            double bondVal = bond.valueBond(currentInterestRate, currentTime, currentInflationRate);
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
    }


    private double totalLiabilitiesAtTime(double time) {
        return liabilities.stream().filter(liability -> liability.dueDate == time)
                .map(liability -> liability.amount).mapToDouble(Double::doubleValue).sum();
    }



    public static Action<PensionFund> payLiabilities(double time) {
      return Action.create(PensionFund.class, pensionFund -> {
          double totalLiabilities = pensionFund.totalLiabilitiesAtTime(time);
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
                        pensionFund.calculatePortfolioValueAndDuration(time);
                        double length = liability.dueDate - time;
//                        System.out.println("/home/sophie/Documents/uni/project/git_folder/bash/activate_run_python.sh" + " " + Double.toString(pensionFund.currentInterestRate) + " " +
//                                Double.toString(pensionFund.currentInflationRate) + " " + Double.toString(pensionFund.currentDuration) + " " + Double.toString(length) + " "
//                                + Double.toString(pensionFund.currentValue) + " " + Double.toString(liability.amount) + " " + Double.toString(pensionFund.cashVal) + " " + pensionFund.strategy.toString());

                        ProcessBuilder processBuilder = new ProcessBuilder("/home/sophie/Documents/uni/project/git_folder/bash/activate_run_python.sh", Double.toString(pensionFund.currentInterestRate),
                                Double.toString(pensionFund.currentInflationRate), Double.toString(pensionFund.currentDuration), Double.toString(length), Double.toString(pensionFund.currentValue),
                                Double.toString(liability.amount), Double.toString(pensionFund.cashVal), pensionFund.strategy.toString());
                        processBuilder.redirectErrorStream(true);
                        Process process = null;
                        String results;
                        try {
                            process = processBuilder.start();
                            results = IOUtils.toString(process.getInputStream(), "UTF-8");
                        } catch (IOException e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                        int exitCode;
                        try {
                            exitCode = process.waitFor();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        if (exitCode == 0) {
                            String[] resultsArray = results.split(",");
                            double requiredLength = Double.parseDouble(resultsArray[0]);
                            double requiredAmount = Double.parseDouble(resultsArray[1]);
                            // This calculates the amount of bonds we need to buy now with current interest rates
                            if (requiredAmount > 1e-06) { // adds a small amount of tolerance
//                                System.out.println("Current cash amount: " + pensionFund.cashVal);
//                                System.out.println("Purchasing Bond for: " + requiredAmount);
                                Bond newBond = new InterestBond(time + Math.round(requiredLength), pensionFund.currentInterestRate, requiredAmount);
                                pensionFund.cashVal -= requiredAmount;
                                pensionFund.getLinks(Links.MarketLink.class).send(Messages.PurchaseBonds.class, (msg, link) -> {
                                    msg.bondToPurchase = newBond;
                                });
                                pensionFund.portfolio.add(newBond);
                            }
                            pensionFund.calculatePortfolioValueAndDuration(time); // Updates current value with the new bond todo can probably optimise this
//                            System.out.println("bond end times: " + pensionFund.portfolio.stream().map(Bond::getEndTime).collect(Collectors.toList()));
                        }

                    }
            );
        });
    }
}

