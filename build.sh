#sdk use java 21-graal
mvn clean package
native-image --module-path target/git4j-1.0-SNAPSHOT.jar --module git4j