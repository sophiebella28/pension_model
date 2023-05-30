#!/bin/bash
cd ../java_model
mvn -s settings.xml clean compile package
java -jar target/simudyne-maven-java-1.0-SNAPSHOT.jar --model-name "Pension Fund Model" --input-path /home/sophie/Documents/uni/project/git_folder/java_model/input_files/

