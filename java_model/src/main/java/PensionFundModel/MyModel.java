package PensionFundModel;

import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.GlobalState;
import simudyne.core.abm.Group;
import simudyne.core.abm.Sequence;
import simudyne.core.annotations.Input;
import simudyne.core.annotations.Variable;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//This is your main model class where you define the components of your model, including the GlobalState, setup, and step.
public class MyModel extends AgentBasedModel<MyModel.Globals> {

    private static final String COMMA_DELIMITER = ",";

    @Input(name = "Use Downward Curve")
    public boolean useDownwardCurve;

    @Input(name = "Initial Interest Rate")
    public double initInterest;

    @Input(name = "Initial Inflation Rate")
    public double initInflation;

    //Globals stores all of your variables and data structures that you want your agents to be able to access
    //Store information here that is system-level knowledge (ie - # of Agents or static variables)
    public static class Globals extends GlobalState {
    // List of global variables that I want
    // - Number of pension funds
    // - DB of pension fund - get rid of later but good for now
    // - DB time period or something?????????????
    // Number of pension funds
    @Input(name = "Number of Duration Matching Funds")
    public int nmDurationPensionFunds = 1;

    @Input(name = "Number of Value Matching Funds")
    public int nmValuePensionFunds = 1;

    @Input(name = "Number of 1Y Value Matching Funds")
    public int nmShortValuePensionFunds = 1;

    @Input(name = "Number of Ineq Duration Matching Funds")
    public int nmIneqDurationPensionFunds = 1;

    public double time;
    @Input(name = "Time Step in ticks")
    public long timeStep = 1;

    @Input(name = "Drift Short Term Interest Rates")
    public double driftShortTerm = 0.1;

    @Input(name = "Volatility Short Term Interest Rates")
    public double volatilityShortTerm = 0.001;

    public List<Double> thetas = new ArrayList<>();


}

    @Override
    public void init() {
        registerAgentTypes(PensionFund.class, BondIssuer.class);
        registerLinkTypes(Links.MarketLink.class);
        registerLinkTypes(Links.BondPurchaseLink.class);
    }

    //Define your agent groups and connections in the setup. This is where the environment and agents are generated
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
        // - controller of interest rates or something idk - maybe this stays in the bond controller

        // Generates given number of pension funds
        Group<PensionFund> durationPensionFundGroup = generateGroup(PensionFund.class, getGlobals().nmDurationPensionFunds, agent -> {agent.strategy = Strategy.DURATION_MATCHING.toString();});
        Group<PensionFund> valuePensionFundGroup = generateGroup(PensionFund.class, getGlobals().nmValuePensionFunds, agent -> {agent.strategy = Strategy.VALUE_MATCHING.toString();});
        Group<PensionFund> shortValuePensionFundGroup = generateGroup(PensionFund.class, getGlobals().nmShortValuePensionFunds, agent -> {agent.strategy = Strategy.SHORT_VALUE_MATCHING.toString();});
        Group<PensionFund> ineqDurationPensionFundGroup = generateGroup(PensionFund.class, getGlobals().nmIneqDurationPensionFunds, agent -> {agent.strategy = Strategy.INEQ_DURATION_MATCHING.toString();});

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

        // Fully connects pension funds with bond issuer using purchase links - used to record whether a pension fund made a purchase that tick
        durationPensionFundGroup.fullyConnected(bondIssuerGroup, Links.BondPurchaseLink.class);
        valuePensionFundGroup.fullyConnected(bondIssuerGroup, Links.BondPurchaseLink.class);
        shortValuePensionFundGroup.fullyConnected(bondIssuerGroup, Links.BondPurchaseLink.class);
        ineqDurationPensionFundGroup.fullyConnected(bondIssuerGroup, Links.BondPurchaseLink.class);


        super.setup();
    }

    // Define the logical sequence of behaviors that should be executed at each time step.
    // Actions can be standalone in a run sequence or contain message passing phases.
    @Override
    public void step() {
        // System.out.println("NEW STEP, time = " + getGlobals().time);
        super.step();
        // a time variable so that current tick doesnt need to be passed around - hasnt been fully refactored into the code
        getGlobals().time = getContext().getTick() * getGlobals().timeStep;
        Sequence payCoupons = Sequence.create(BondIssuer.giveCoupons(getGlobals().time), PensionFund.receiveCoupons(getGlobals().time)); // TODO: look into whether I can access time non statically
        // pays coupons to bond holders
        Sequence payLiabilities = Sequence.create(PensionFund.payLiabilities(getGlobals().time), BondIssuer.receiveSoldBonds());
        // pension fund pays its liabilities
        Sequence updateInterest = Sequence.create(BondIssuer.updateInterest(getGlobals().thetas.get((int) Math.round(getGlobals().time))), PensionFund.receiveInterestRates());
        Sequence performHedges = Sequence.create(PensionFund.buyHedges(getGlobals().time, getGlobals().timeStep), BondIssuer.receiveHedges());
        //
        // 1. government gives funds their interest + whatever values they're owed depending on bonds
        // 2. funds pay their liabilities
        // 3. increment values of bonds + send new values to pension fund (this should LATER be modelled by a synthetic market)
        //
        // 4. funds assess how much money they need to make for their projected liabilities
        //    in this step they will need to value the derivatives according to some formula
        //    but that is a NEXT WEEK problem
        //
        // + buy the necessary instruments
        //     from the government
        //System.out.println("pay coupons");
        run(payCoupons);
        //System.out.println("update interest");
        run(updateInterest);
        //System.out.println("pay liabilities");
        run(payLiabilities);
        //System.out.println("perform hedges");
        run(performHedges);


    }


}
