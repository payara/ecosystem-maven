#!groovy

pipeline {

    agent {
        label 'general-purpose'
    }
    tools {
        maven "maven-3.6.3"
    }
    stages {

        stage('Build payara-maven-plugins-common') {
            environment {
                JAVA_HOME = tool("zulu-11")
                PATH = "${env.JAVA_HOME}/bin:${env.PATH}"
                MAVEN_OPTS = '-Xmx2G -Djavax.net.ssl.trustStore=${JAVA_HOME}/jre/lib/security/cacerts'
                payaraBuildNumber = "${BUILD_NUMBER}"
            }
            steps {
                script {
                    sh '''
                    ls -lrt
                    cd payara-maven-plugins-common
                    echo *#*#*#*#*#*#*#*#*#*#*#*#  Building SRC  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#
                    mvn -B -V -ff -e clean install --strict-checksums \
                        com.github.spotbugs:spotbugs-maven-plugin:spotbugs \
                        -Dfindbugs.skip=false -Djavadoc.skip=true -Dsource.skip=true \
                        -Dspotbugs.effort=max -Dspotbugs.htmlOutput=true
                    echo *#*#*#*#*#*#*#*#*#*#*#*#    Built SRC   *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#
                    '''
                }
            }
        }
        stage('Build payara-micro-maven-plugin') {
            environment {
                JAVA_HOME = tool("zulu-11")
                PATH = "${env.JAVA_HOME}/bin:${env.PATH}"
                MAVEN_OPTS = '-Xmx2G -Djavax.net.ssl.trustStore=${JAVA_HOME}/jre/lib/security/cacerts'
                payaraBuildNumber = "${BUILD_NUMBER}"
            }
            steps {
                script {
                    sh '''
                    ls -lrt
                    cd payara-micro-maven-plugin
                    echo *#*#*#*#*#*#*#*#*#*#*#*#  Building SRC  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#
                    mvn -B -V -ff -e clean install --strict-checksums \
                        com.github.spotbugs:spotbugs-maven-plugin:spotbugs \
                        -Dfindbugs.skip=false -Djavadoc.skip=true -Dsource.skip=true \
                        -Dspotbugs.effort=max -Dspotbugs.htmlOutput=true
                    echo *#*#*#*#*#*#*#*#*#*#*#*#    Built SRC   *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#
                    '''
                }
            }
        }
        stage('Build payara-cloud-maven-plugin') {
            environment {
                JAVA_HOME = tool("zulu-11")
                PATH = "${env.JAVA_HOME}/bin:${env.PATH}"
                MAVEN_OPTS = '-Xmx2G -Djavax.net.ssl.trustStore=${JAVA_HOME}/jre/lib/security/cacerts'
                payaraBuildNumber = "${BUILD_NUMBER}"
            }
            steps {
                script {
                    sh '''
                    ls -lrt
                    cd payara-cloud-maven-plugin
                    echo *#*#*#*#*#*#*#*#*#*#*#*#  Building SRC  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#
                    mvn -B -V -ff -e clean install --strict-checksums \
                        com.github.spotbugs:spotbugs-maven-plugin:spotbugs \
                        -Dfindbugs.skip=false -Djavadoc.skip=true -Dsource.skip=true \
                        -Dspotbugs.effort=max -Dspotbugs.htmlOutput=true
                    echo *#*#*#*#*#*#*#*#*#*#*#*#    Built SRC   *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#
                    '''
                }
            }
        }
        stage('Build payara-micro-maven-archetype') {
            environment {
                JAVA_HOME = tool("zulu-11")
                PATH = "${env.JAVA_HOME}/bin:${env.PATH}"
                MAVEN_OPTS = '-Xmx2G -Djavax.net.ssl.trustStore=${JAVA_HOME}/jre/lib/security/cacerts'
                payaraBuildNumber = "${BUILD_NUMBER}"
            }
            steps {
                script {
                    sh '''
                    ls -lrt
                    cd payara-micro-maven-archetype
                    echo *#*#*#*#*#*#*#*#*#*#*#*#  Building SRC  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#
                    mvn -B -V -ff -e clean install --strict-checksums \
                        com.github.spotbugs:spotbugs-maven-plugin:spotbugs \
                        -Dfindbugs.skip=false -Djavadoc.skip=true -Dsource.skip=true \
                        -Dspotbugs.effort=max -Dspotbugs.htmlOutput=true
                    echo *#*#*#*#*#*#*#*#*#*#*#*#    Built SRC   *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#
                    '''
                }
            }
        }
        stage('Aggregate Reports') {
            steps {
                script {

sh '''#!/bin/bash

# Define the directory where you want to copy the SpotBugs HTML files
OUTPUT_DIR="./all-spotbugs-html-reports"

# Create the output directory if it doesn't exist
mkdir -p "$OUTPUT_DIR"

# Find all spotbugs.html files and process each
find . -name "spotbugs.html" | while read -r file; do
    # Extract the project name (assuming the project name is the immediate directory containing the spotbugs.html file)
    project_name=$(basename "$(dirname "$(dirname "$file")")")
    
    # Copy the spotbugs.html file to the output directory with a new name
    cp "$file" "$OUTPUT_DIR/${project_name}-spotbugs.html"
done

echo "All spotbugs.html files have been copied to $OUTPUT_DIR with their project names."
'''
sh '''#!/bin/bash

# Define the directory where you want to copy the SpotBugs HTML files
OUTPUT_DIR="./all-spotbugs-xml-reports"

# Create the output directory if it doesn't exist
mkdir -p "$OUTPUT_DIR"

# Find all spotbugs.html files and process each
find . -name "spotbugsXml.xml" | while read -r file; do
    # Extract the project name (assuming the project name is the immediate directory containing the spotbugs.html file)
    project_name=$(basename "$(dirname "$(dirname "$file")")")
    
    # Copy the spotbugs.html file to the output directory with a new name
    cp "$file" "$OUTPUT_DIR/${project_name}-spotbugsXml.xml"
done

echo "All spotbugsXml.xml files have been copied to $OUTPUT_DIR with their project names."
'''
                }
            }
        }

        stage('Publish SpotBugs Results') {
            steps {
                // Publish SpotBugs results using the Warnings NG plugin
                recordIssues tools: [spotBugs(pattern: 'all-spotbugs-xml-reports/*.xml')]
            }
        }
    }
}
