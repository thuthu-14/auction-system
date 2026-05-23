# Copilot Instructions

This is a Java 17 Maven project for a JavaFX auction system.

- Main source code is under `src/main/java`; do not use legacy paths such as `src/server`.
- UI resources are under `src/main/resources/fxml` and `src/main/resources/CSS`.
- Run verification with `.\mvnw.cmd verify`.
- The runnable client artifact is `target/auction-system-1.0-SNAPSHOT-client.jar`.
- Avoid committing runtime files such as `data/*.db`, `logs/`, and generated `target/` output.
- Prefer existing controller/service patterns before adding new abstractions.
