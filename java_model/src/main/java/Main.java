
import MyFirstModel.MyModel;
import simudyne.nexus.Server;

public class Main {
  public static void main(String[] args) {

    Server.register("My First Model", MyModel.class);

    Server.run(args);
  }
}
