package PensionFundModel;

import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.GlobalState;
import simudyne.core.abm.Group;
import simudyne.core.abm.Sequence;
import simudyne.core.annotations.Input;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MyModel extends AgentBasedModel<MyModel.Globals> {
    // Some variables defined globally to allow them to be set via json file
    @Input(name = "Use Downward Curve")
    public boolean useDownwardCurve = false;

    @Input(name = "Initial Interest Rate")
    public double initInterest = 0.02;

    @Input(name = "Initial Inflation Rate")
    public double initInflation = 0.03;

    //Globals stores all of your variables and data structures that you want your agents to be able to access
    //Store information here that is system-level knowledge (ie - # of Agents or static variables)
    public static class Globals extends GlobalState {

    @Input(name = "Number of Funds Per Strategy")
    public int nmPensionFundsPerStrategy = 1;

    public double time;

    @Input(name = "Drift Interest Rates")
    public double interestDrift = 0.1;

    @Input(name = "Volatility Interest Rates")
    public double interestVolatility = 0.001;

    @Input(name = "Constant Term Inflation Rates")
    public double inflationConstant = 0.007204;

    @Input(name = "Drift Inflation Rates")
    public double inflationDrift =  0.748283;

    @Input(name = "Volatility Inflation Rates")
    public double inflationVolatility = 0.013;

    @Input(name = "Inflation-Interest Correlation")
    public double corr = 0.58;

    public List<Double> thetas = new ArrayList<>();


}

    @Override
    public void init() {
        registerAgentTypes(PensionFund.class, BondIssuer.class);
        registerLinkTypes(Links.MarketLink.class);
        registerLinkTypes(Links.BondPurchaseLink.class);
    }

    @Override
    public void setup() {
        String thetaFileName;
        if (useDownwardCurve) {
            thetaFileName = "downward_curve.csv";
        } else {
            thetaFileName = "upward_curve.csv";
        }

        try (BufferedReader br = new BufferedReader(new FileReader("theta_files/" + thetaFileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                getGlobals().thetas.add(Double.parseDouble(line));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Types of agent
        // - pension fund
        // - bond controller

        // Generates given number of pension funds
        Group<PensionFund> durationPensionFundGroup = generateGroup(PensionFund.class, getGlobals().nmPensionFundsPerStrategy, agent -> {agent.strategy = Strategy.DURATION_MATCHING.toString();
        agent.isIndex = false;});
        Group<PensionFund> valuePensionFundGroup = generateGroup(PensionFund.class, getGlobals().nmPensionFundsPerStrategy, agent -> {agent.strategy = Strategy.VALUE_MATCHING.toString();
            agent.isIndex = false;});
        Group<PensionFund> shortValuePensionFundGroup = generateGroup(PensionFund.class, getGlobals().nmPensionFundsPerStrategy, agent -> {agent.strategy = Strategy.SHORT_VALUE_MATCHING.toString();
            agent.isIndex = false;});
        Group<PensionFund> ineqDurationPensionFundGroup = generateGroup(PensionFund.class, getGlobals().nmPensionFundsPerStrategy, agent -> {agent.strategy = Strategy.INEQ_DURATION_MATCHING.toString();
            agent.isIndex = false;});
        Group<PensionFund> ineqDurationIndexPensionFundGroup = generateGroup(PensionFund.class, getGlobals().nmPensionFundsPerStrategy, agent -> {agent.strategy = Strategy.INEQ_DURATION_MATCHING.toString();
            agent.isIndex = true;});
        Group<PensionFund> durationIndexPensionFundGroup = generateGroup(PensionFund.class, getGlobals().nmPensionFundsPerStrategy, agent -> {agent.strategy = Strategy.DURATION_MATCHING.toString();
            agent.isIndex = true;});
        Group<PensionFund> valueIndexPensionFundGroup = generateGroup(PensionFund.class, getGlobals().nmPensionFundsPerStrategy, agent -> {agent.strategy = Strategy.VALUE_MATCHING.toString();
            agent.isIndex = true;});
        Group<PensionFund> shorIndextValuePensionFundGroup = generateGroup(PensionFund.class, getGlobals().nmPensionFundsPerStrategy, agent -> {agent.strategy = Strategy.SHORT_VALUE_MATCHING.toString();
            agent.isIndex = true;});

        Group<BondIssuer> bondIssuerGroup = generateGroup(BondIssuer.class, 1, agent ->
        {
            agent.interestRate = initInterest;
            agent.inflationRate = initInflation;
        });
        // Fully connects pension funds with bond issuer
        durationPensionFundGroup.fullyConnected(bondIssuerGroup, Links.MarketLink.class);
        bondIssuerGroup.fullyConnected(durationPensionFundGroup, Links.MarketLink.class);

        valuePensionFundGroup.fullyConnected(bondIssuerGroup, Links.MarketLink.class);
        bondIssuerGroup.fullyConnected(valuePensionFundGroup, Links.MarketLink.class);

        shortValuePensionFundGroup.fullyConnected(bondIssuerGroup, Links.MarketLink.class);
        bondIssuerGroup.fullyConnected(shortValuePensionFundGroup, Links.MarketLink.class);

        ineqDurationPensionFundGroup.fullyConnected(bondIssuerGroup, Links.MarketLink.class);
        bondIssuerGroup.fullyConnected(ineqDurationPensionFundGroup, Links.MarketLink.class);

        ineqDurationIndexPensionFundGroup.fullyConnected(bondIssuerGroup, Links.MarketLink.class);
        bondIssuerGroup.fullyConnected(ineqDurationIndexPensionFundGroup, Links.MarketLink.class);

        durationIndexPensionFundGroup.fullyConnected(bondIssuerGroup, Links.MarketLink.class);
        bondIssuerGroup.fullyConnected(durationIndexPensionFundGroup, Links.MarketLink.class);

        valueIndexPensionFundGroup.fullyConnected(bondIssuerGroup, Links.MarketLink.class);
        bondIssuerGroup.fullyConnected(valueIndexPensionFundGroup, Links.MarketLink.class);

        shorIndextValuePensionFundGroup.fullyConnected(bondIssuerGroup, Links.MarketLink.class);
        bondIssuerGroup.fullyConnected(shorIndextValuePensionFundGroup, Links.MarketLink.class);

        // Fully connects pension funds with bond issuer using purchase links - used to record whether a pension fund made a purchase that tick
        durationPensionFundGroup.fullyConnected(bondIssuerGroup, Links.BondPurchaseLink.class);
        valuePensionFundGroup.fullyConnected(bondIssuerGroup, Links.BondPurchaseLink.class);
        shortValuePensionFundGroup.fullyConnected(bondIssuerGroup, Links.BondPurchaseLink.class);
        ineqDurationPensionFundGroup.fullyConnected(bondIssuerGroup, Links.BondPurchaseLink.class);
        durationIndexPensionFundGroup.fullyConnected(bondIssuerGroup, Links.BondPurchaseLink.class);
        ineqDurationIndexPensionFundGroup.fullyConnected(bondIssuerGroup, Links.BondPurchaseLink.class);
        valueIndexPensionFundGroup.fullyConnected(bondIssuerGroup, Links.BondPurchaseLink.class);
        shorIndextValuePensionFundGroup.fullyConnected(bondIssuerGroup, Links.BondPurchaseLink.class);

        super.setup();
    }

    @Override
    public void step() {
        super.step();
        // a time variable so that context isn't required in the agents
        getGlobals().time = getContext().getTick();
        Sequence payCoupons = Sequence.create(BondIssuer.giveCoupons(getGlobals().time), PensionFund.receiveCoupons(getGlobals().time));
        // pays coupons to bond holders
        Sequence payLiabilities = Sequence.create(PensionFund.payLiabilities(getGlobals().time), BondIssuer.receiveSoldBonds());
        // pension fund pays its liabilities
        Sequence updateInterest = Sequence.create(BondIssuer.updateInterest(getGlobals().thetas.get((int) Math.round(getGlobals().time))), PensionFund.receiveInterestRates(getGlobals().time));
        Sequence performHedges = Sequence.create(PensionFund.buyHedges(getGlobals().time), BondIssuer.receiveHedges());
        //
        // 1. government gives funds their interest + whatever values they're owed depending on bonds
        // 2. funds pay their liabilities
        // 3. increment values of bonds + send new values to pension fund (this should LATER be modelled by a synthetic market)
        //
        // 4. funds assess how much money they need to make for their projected liabilities
        //    in this step they will need to value the derivatives according to some formula
        //
        // + buy the necessary instruments
        //     from the government
        run(payCoupons);
        run(updateInterest);
        run(payLiabilities);
        run(performHedges);


    }


}
