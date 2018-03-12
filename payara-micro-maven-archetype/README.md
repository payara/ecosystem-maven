# Payara Micro Quickstart Maven Archetype

## Summary
The project is a Maven archetype for Payara Micro application.

## Installation

To install the archetype in your local repository execute following commands:

```sh
$ git clone https://github.com/payara/ecosystem-maven.git
$ cd payara-micro-maven-archetype
$ mvn clean install
```

## Create a project

```sh
$ mvn archetype:generate \
    -DarchetypeGroupId=fish.payara.maven.archetypes \
    -DarchetypeArtifactId=payara-micro-maven-archetype \
    -DarchetypeVersion=1.0-SNAPSHOT \
    -DgroupId=fish.payara.micro \
    -DartifactId=micro-sample \
    -Dversion=1.0-SNAPSHOT \
    -Dpackage=fish.payara.micro.sample \
    -Darchetype.interactive=false
```

## Run the project

```sh
$ mvn payara-micro:start
```