# Payara Micro Quickstart Maven Archetype

## Summary
The project is a Maven archetype for Payara Micro application.

## Create a project

```sh
$ mvn archetype:generate -DarchetypeGroupId=fish.payara.maven.archetypes -DarchetypeArtifactId=payara-micro-maven-archetype -DarchetypeVersion=1.0 -DgroupId=fish.payara.micro -DartifactId=micro-sample -Dversion=1.0-SNAPSHOT -Dpackage=fish.payara.micro.sample -Darchetype.interactive=false
```

## Run the project

```sh
$ mvn payara-micro:start
```