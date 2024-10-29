# Maven extension configuring the Develocity build cache configuration for the Hibernate Search project

[![Version](https://img.shields.io/maven-central/v/org.hibernate.infra.develocity/hibernate-develocity-maven-extension?logo=apache-maven&style=for-the-badge)](https://central.sonatype.com/artifact/org.hibernate.infra.develocity/hibernate-develocity-maven-extension)

## About

This Maven extension is designed to configure the Develocity build cache for the Hibernate Search project.

## Developing

The reference documentation for the API can be found [here](https://docs.gradle.com/enterprise/maven-extension/api/).

When working on caching new goals, you can obtain a debug output with the following command:

```
./mvnw -DskipTests -DskipITs -Dorg.slf4j.simpleLogger.log.gradle.goal.cache=debug -Dorg.slf4j.simpleLogger.log.io.hibernate.search.develocity=debug -e clean install
```

This command should be run on a single module on the Hibernate Search project for easier debugging.

Note: the `clean` goal is important.
The cache won't be populated otherwise.

You can also get some information about the generation of the cache key with `-Dorg.slf4j.simpleLogger.log.gradle.goal.fingerprint=trace`.
