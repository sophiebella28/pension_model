package PensionFundModel;

import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.GlobalState;
import simudyne.core.abm.Group;
import simudyne.core.abm.Sequence;
import simudyne.core.annotations.Input;

//This is your main model class where you define the components of your model, including the GlobalState, setup, and step.
public class MyModel extends AgentBasedModel<MyModel.Globals> {

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

    public double time;
    @Input(name = "Time Step in ticks")
    public long timeStep = 1;

    @Input(name = "Drift Short Term Interest Rates")
    public double driftShortTerm = 0.02;

    @Input(name = "Volatility Short Term Interest Rates")
    public double volatilityShortTerm = 0.001;


    public double[] thetas = {-0.09592369, -0.06244385, -0.03235271, -0.00548885,  0.01830917,  0.03920281,
                0.0573535,   0.07292267,  0.08607178,  0.09696226,  0.10575556,  0.11261311,
                0.11769636,  0.12116674,  0.1231857,   0.12391468,  0.12351512,  0.12214847,
                0.11997615,  0.11715962,  0.11386031,  0.11023967,  0.10645913,  0.10268015,
                0.09906415,  0.09577258,  0.09296688,  0.09080849,  0.08945885,  0.08907941,
                0.08983161,  0.09187688,  0.09537666,  0.10049241,  0.10738555,  0.11621753,
                0.1271498,   0.14034378,  0.15596093, 0.17416268};


}

    @Override
    public void init() {
        registerAgentTypes(PensionFund.class, BondIssuer.class);
        registerLinkTypes(Links.MarketLink.class);
    }

    //Define your agent groups and connections in the setup. This is where the environment and agents are generated
    @Override
    public void setup() {
        // Types of agent
        // - pension fund
        // - bond controller
        // - controller of interest rates or something idk - maybe this stays in the bond controller

        // Generates given number of pension funds
        Group<PensionFund> durationPensionFundGroup = generateGroup(PensionFund.class, getGlobals().nmDurationPensionFunds, agent -> {agent.strategy = Strategy.DURATION_MATCHING;});
        Group<PensionFund> valuePensionFundGroup = generateGroup(PensionFund.class, getGlobals().nmValuePensionFunds, agent -> {agent.strategy = Strategy.VALUE_MATCHING;});
        Group<BondIssuer> bondIssuerGroup = generateGroup(BondIssuer.class, 1);
        // Fully connects pension funds with bond issuer
        durationPensionFundGroup.fullyConnected(bondIssuerGroup, Links.MarketLink.class);
        bondIssuerGroup.fullyConnected(durationPensionFundGroup, Links.MarketLink.class);
        valuePensionFundGroup.fullyConnected(bondIssuerGroup, Links.MarketLink.class);
        bondIssuerGroup.fullyConnected(valuePensionFundGroup, Links.MarketLink.class);
        super.setup();
    }

    // Define the logical sequence of behaviors that should be executed at each time step.
    // Actions can be standalone in a run sequence or contain message passing phases.
    @Override
    public void step() {
        System.out.println("NEW STEP, time = " + getGlobals().time);
        super.step();
        // a time variable so that current tick doesnt need to be passed around - hasnt been fully refactored into the code
        getGlobals().time = getContext().getTick() * getGlobals().timeStep;
        Sequence payCoupons = Sequence.create(BondIssuer.giveCoupons(getGlobals().time), PensionFund.receiveCoupons(getGlobals().time)); // TODO: look into whether I can access time non statically
        // pays coupons to bond holders
        Sequence payLiabilities = Sequence.create(PensionFund.payLiabilities(getGlobals().time), BondIssuer.receiveSoldBonds());
        // pension fund pays its liabilities
        Sequence updateInterest = Sequence.create(BondIssuer.updateInterest(getGlobals().thetas[(int) Math.round(getGlobals().time)]), PensionFund.receiveInterestRates());
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
        System.out.println("pay coupons");
        run(payCoupons);
        System.out.println("pay liabilities");
        run(payLiabilities);
        System.out.println("update interest");
        run(updateInterest);
        System.out.println("perform hedges");
        run(performHedges);


    }


}