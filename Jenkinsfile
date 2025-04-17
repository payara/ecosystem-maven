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
                    mvn clean install
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
                    mvn clean install
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
                    mvn clean install
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
                    mvn clean install
                    echo *#*#*#*#*#*#*#*#*#*#*#*#    Built SRC   *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#
                    '''
                }
            }
        }
        stage('Build payara-server-maven-plugin') {
            environment {
                JAVA_HOME = tool("zulu-17")
                PATH = "${env.JAVA_HOME}/bin:${env.PATH}"
                MAVEN_OPTS = '-Xmx2G -Djavax.net.ssl.trustStore=${JAVA_HOME}/jre/lib/security/cacerts'
                payaraBuildNumber = "${BUILD_NUMBER}"
            }
            steps {
                script {
                    sh '''
                    ls -lrt
                    cd payara-server-maven-plugin
                    echo *#*#*#*#*#*#*#*#*#*#*#*#  Building SRC  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#
                    mvn clean install
                    echo *#*#*#*#*#*#*#*#*#*#*#*#    Built SRC   *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#
                    '''
                }
            }
        }
    }
}
