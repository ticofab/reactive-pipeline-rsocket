# legacy-processor-2

This app receives data from a RSocket faucet and publishes items to a Kafka topic after processing.

```
RSocket --> internal logic --> Kafka
```

Launch with

```
mvn clean compile exec:java
```
