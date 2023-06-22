package PensionFundModel;

import org.apache.commons.io.IOUtils;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

import java.io.IOException;
import java.sql.SQLOutput;
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

    @Variable
    public boolean isIndex;

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
          // If there is a liability to pay, sell all bonds in the portfolio as we have reached the end of the simulation
          while (totalLiabilities > 0.0 && iterator.hasNext()) {
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
            if (time > 0.0 && pensionFund.cashVal > 0.0) {
                pensionFund.cashVal += pensionFund.cashVal * pensionFund.currentInterestRate;
            }
            pensionFund.getMessagesOfType(Messages.CouponPayment.class).forEach(coupon -> pensionFund.cashVal += coupon.coupons);
            pensionFund.portfolio.removeIf(bond -> bond.getEndTime() <= time);

            // pension fund receives interest based on previous interest rates
        });
    }

    public static Action<PensionFund> receiveInterestRates(double time) {
        return Action.create(PensionFund.class, pensionFund -> {
            pensionFund.getMessagesOfType(Messages.InterestUpdate.class).stream().forEach(msg -> {
                pensionFund.currentInterestRate = msg.interestRate;
                pensionFund.currentInflationRate = msg.inflationRate;
            });
            if (time > 0) {
                pensionFund.liabilities.forEach(liability -> liability.updateLiabilityAmount(pensionFund.currentInflationRate));
                // doesn't increase pension amount at first timestep
            }
        });
    }

    public static Action<PensionFund> buyHedges(double time) {
        return Action.create(PensionFund.class, pensionFund -> {
            pensionFund.calculatePortfolioValueAndDuration(time);
            pensionFund.currentLiabilityVal = pensionFund.liabilities.stream().map(liability -> liability.valueLiability(pensionFund.currentInflationRate, pensionFund.currentInterestRate, time)).reduce(Double::sum).orElse(0.0);
            if (!pensionFund.liabilities.isEmpty()) {
                double length = pensionFund.liabilities.stream().mapToDouble(liability -> liability.dueDate).average().orElse(0.0) - time;
                double yield;
                double couponRate;
                if (pensionFund.isIndex) {
                    yield = (pensionFund.currentInterestRate - pensionFund.currentInflationRate) / (1 + pensionFund.currentInflationRate);
                    couponRate = pensionFund.currentInterestRate * 0.5;
                } else {
                    yield = pensionFund.currentInterestRate;
                    couponRate = pensionFund.currentInterestRate;
                }

                System.out.println("/home/sophie/Documents/uni/project/git_folder/bash/activate_run_python.sh" + " " + Double.toString(pensionFund.currentDuration) + " " + Double.toString(length) + " "
                        + Double.toString(pensionFund.currentValue) + " " + Double.toString(pensionFund.currentLiabilityVal) + " " + pensionFund.strategy.toString() + " " + Double.toString(yield) + " " + Double.toString(couponRate));

                ProcessBuilder processBuilder = new ProcessBuilder("/home/sophie/Documents/uni/project/git_folder/bash/activate_run_python.sh", Double.toString(pensionFund.currentDuration),
                        Double.toString(length), Double.toString(pensionFund.currentValue), Double.toString(pensionFund.currentLiabilityVal), pensionFund.strategy.toString(), Double.toString(yield), Double.toString(couponRate));
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
                        Bond newBond;
                        if (pensionFund.isIndex) {
                            newBond = new IndexBond(time + Math.round(requiredLength), pensionFund.currentInterestRate, pensionFund.currentInflationRate + 1, requiredAmount, time);
                        } else {
                            newBond = new InterestBond(time + Math.round(requiredLength), pensionFund.currentInterestRate, requiredAmount);
                        }
                        pensionFund.cashVal -= requiredAmount;
                        pensionFund.getLinks(Links.BondPurchaseLink.class).send(Messages.PurchaseBonds.class, (msg, link) -> {
                            msg.bondToPurchase = newBond;
                        });
                        pensionFund.portfolio.add(newBond);
                    }
                    pensionFund.calculatePortfolioValueAndDuration(time); // Updates current value with the new bond
                }
            }
        });
    }
}

