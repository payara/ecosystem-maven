#!groovy

pipeline {

    agent {
        label 'general-purpose'
    }
    tools {
        maven "maven-3.6.3"
    }
    stages {


        stage('Build payara-micro-maven-plugin') {
            environment {
                JAVA_HOME = tool("zulu-8")
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
                        -Djavadoc.skip -Dsource.skip
                    echo *#*#*#*#*#*#*#*#*#*#*#*#    Built SRC   *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#
                    '''
                }
            }
        }

        stage('Build payara-micro-maven-archetype') {
            environment {
                JAVA_HOME = tool("zulu-8")
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
                        -Djavadoc.skip -Dsource.skip
                    echo *#*#*#*#*#*#*#*#*#*#*#*#    Built SRC   *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#
                    '''
                }
            }
        }
    }
}
