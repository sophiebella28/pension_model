
import PensionFundModel.MyModel;
import simudyne.nexus.Server;

public class Main {
  public static void main(String[] args) {

    Server.register("Pension Fund Model", MyModel.class);

    Server.run(args);
  }

}
