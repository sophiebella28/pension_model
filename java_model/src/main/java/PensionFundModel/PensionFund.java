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
    public double currentDuration;

    @Variable
    public double currentValue;
    private List<Bond> portfolio;
    private final List<Liability> liabilities;

    @Variable
    public String strategy;

    public PensionFund() {
        this.portfolio = new ArrayList<Bond>();
        this.liabilities = new ArrayList<>(Arrays.asList(new Liability(3000, 10.0, 0.02)));
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
            if (time > 0.0) {
                pensionFund.cashVal += pensionFund.cashVal * pensionFund.currentInterestRate;
            }
            pensionFund.getMessagesOfType(Messages.CouponPayment.class).forEach(coupon -> pensionFund.cashVal += coupon.coupons);
            pensionFund.portfolio.removeIf(bond -> bond.getEndTime() <= time);

            // pension fund receives interest based on previous interest rates
        });
    }

    public static Action<PensionFund> receiveInterestRates() {
        return Action.create(PensionFund.class, pensionFund -> {
            pensionFund.getMessagesOfType(Messages.InterestUpdate.class).stream().forEach(msg -> {
                pensionFund.currentInterestRate = msg.interestRate;
                pensionFund.currentInflationRate = msg.inflationRate;
            });
            pensionFund.liabilities.forEach(liability -> liability.updateLiabilityAmount(pensionFund.currentInflationRate));
            pensionFund.currentLiabilityVal = pensionFund.liabilities.stream().map(liability -> liability.amount).reduce(Double::sum).orElse(0.0);
        });
    }

    public static Action<PensionFund> buyHedges(double time, double timestep) {
        return Action.create(PensionFund.class, pensionFund -> {
            pensionFund.calculatePortfolioValueAndDuration(time);
            pensionFund.liabilities.forEach(
                    liability -> {
                        //TODO THIS WONT WORK AS SOON!!!!! AS SOON AS I HAVE MORE THAN ONE LIABILITY
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
                                // System.out.println("duration of new bond: " + newBond.calculateDuration(time, pensionFund.currentInterestRate, pensionFund.currentInflationRate));
                                pensionFund.cashVal -= requiredAmount;
                                pensionFund.getLinks(Links.BondPurchaseLink.class).send(Messages.PurchaseBonds.class, (msg, link) -> {
                                    msg.bondToPurchase = newBond;
                                });
                                pensionFund.portfolio.add(newBond);
                            }
                            pensionFund.calculatePortfolioValueAndDuration(time); // Updates current value with the new bond todo can probably optimise this
                            // System.out.println("Current Duration:" + pensionFund.currentDuration);
                        }
                        System.out.println("Current Bond Portfolio " + pensionFund.strategy + pensionFund.portfolio.stream().map(bond -> (bond.getEndTime() - time)).collect(Collectors.toList()));
                    }
            );
        });
    }
}

