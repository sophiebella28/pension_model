# Simudyne Maven Java Skeleton

This repository serves as a simple working example of using Simudyne from Java using the Maven build tool. You will need
Maven installed locally yourself, or provided through an IDE such as Eclipse or IntelliJ. Because Simudyne jars are
served from an authenticated artifact repository, you will need to provide information to Maven on where this repository
is located and your credentials.

These settings are located in a `settings.xml` file in this repository. However, these settings are usually located at
`~/.m2/settings.xml`. You can pass the `-s settings.xml` option to point maven at these settings yourself manually,
configure your IDE with a specific settings.xml file, or you may merge the `settings.xml` into your own existing
`settings.xml` if you are already a Maven user. **You need to insert your own credentials as provided by Simudyne into
the `settings.xml` file.** See our [Docs](https://docs.simudyne.com/) for info on Access

## Running the project

Included in the pom.xml is configuration for the `exec-maven-plugin`, so you can run the compiled project via
`mvn -s settings.xml clean compile exec:java`, which will compile the project and then call the Main.main method.

## Additional Info

For information on distribution, usage, deployment, and more please refer to documentation at our [Docs](https://docs.simudyne.com/)
