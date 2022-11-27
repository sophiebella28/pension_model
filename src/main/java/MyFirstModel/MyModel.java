package MyFirstModel;

import scala.collection.Seq;
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
    // List of global variables that i want
    // - Number of pension funds
    // - DB of pension fund - get rid of later but good for now
    // - DB time period or something?????????????
    // Number of pension funds
    @Input(name = "Number of Pension Funds")
    public int nmPensionFunds = 1;
    public double time;
    @Input(name = "Time Step in ticks")
    public long timeStep = 1;

    @Input(name = "Time Step in ticks")
    public double drift = 0.000558;

    @Input(name = "Time Step in ticks")
    public double volatility = 0.027388;
}

    @Override
    public void init() {
        registerAgentTypes();
        registerLinkTypes();
    }

    //Define your agent groups and connections in the setup. This is where the environment and agents are generated
    @Override
    public void setup() {
        // Types of agent
        // - pension fund
        // - bond controller
        // - controller of interest rates or something idk - maybe this stays in the bond controller

        // Generates given number of pension funds
        Group<PensionFund> pensionFundGroup = generateGroup(PensionFund.class, getGlobals().nmPensionFunds);

        Group<BondIssuer> bondIssuerGroup = generateGroup(BondIssuer.class, 1);

        // Fully connects pension funds with bond issuer
        pensionFundGroup.fullyConnected(bondIssuerGroup, Links.MarketLink.class);

        bondIssuerGroup.fullyConnected(pensionFundGroup, Links.MarketLink.class);


        super.setup();
    }

    // Define the logical sequence of behaviors that should be executed at each time step.
    // Actions can be standalone in a run sequence or contain message passing phases.
    @Override
    public void step() {
        super.step();
        // a time variable so that current tick doesnt need to be passed around - hasnt been fully refactored into the code
        getGlobals().time = getContext().getTick() * getGlobals().timeStep;
        Sequence payCoupons = Sequence.create(PensionFund.requestCoupons(getGlobals().time), BondIssuer.giveCoupons); // TODO: look into whether I can access time non statically
        Sequence payLiabilities = Sequence.create(PensionFund.payLiabilities);
        Sequence updateInterest = Sequence.create(BondIssuer.updateInterest, PensionFund.receiveInterestRates(getGlobals().time, getGlobals().timeStep));
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



    }


}
