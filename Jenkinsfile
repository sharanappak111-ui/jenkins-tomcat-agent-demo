pipeline {
    // Run this pipeline on a dedicated Jenkins agent, not on the controller.
    agent { label 'slave' }

    options {
        timestamps()
        timeout(time: 20, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
    }

    parameters {
        choice(
            name: 'DEPLOY_ENV',
            choices: ['dev', 'test', 'prod'],
            description: 'Demo deployment target label (deployment is local to this agent)'
        )
        booleanParam(
            name: 'RUN_TESTS',
            defaultValue: true,
            description: 'Run unit tests in the parallel test stage'
        )
        booleanParam(
            name: 'DEPLOY_TO_TOMCAT',
            defaultValue: false,
            description: 'Copy the WAR to Tomcat on this agent; requires Tomcat to be installed and writable'
        )
        string(
            name: 'APP_VERSION',
            defaultValue: '1.0',
            description: 'Demo version label printed in the pipeline'
        )
    }

    environment {
        APP_NAME = 'demo-webapp'
        // Override this on your agent if Tomcat uses a different location.
        TOMCAT_WEBAPPS_DIR = '/opt/tomcat/webapps'
    }

    stages {
        stage('Checkout') {
            steps {
                echo "Building ${env.APP_NAME}, version ${params.APP_VERSION}"
                checkout scm
            }
        }

        stage('Tool Check') {
            steps {
                sh '''
                    set -eu
                    echo "Agent: $(hostname)"
                    echo "Java:"
                    java -version
                    echo "Maven:"
                    mvn -version
                    echo "Workspace: $WORKSPACE"
                '''
            }
        }

        stage('Parallel Checks') {
            parallel {
                stage('Source Validation') {
                    steps {
                        sh '''
                            set -eu
                            test -f pom.xml
                            test -f src/main/webapp/index.jsp
                            test -f src/main/java/demo/MessageService.java
                            echo "Required project files are present."
                        '''
                    }
                }

                stage('Unit Tests') {
                    when {
                        expression { params.RUN_TESTS }
                    }
                    steps {
                        sh 'mvn -B test'
                    }
                }

                stage('Dependency / Build Metadata') {
                    steps {
                        sh '''
                            set -eu
                            echo "Maven project metadata:"
                            grep -E '<(groupId|artifactId|version|packaging)>' pom.xml || true
                        '''
                    }
                }
            }
        }

        stage('Package WAR') {
            steps {
                sh 'mvn -B package -DskipTests'
                archiveArtifacts artifacts: 'target/*.war', fingerprint: true
            }
        }

        stage('Deploy to Tomcat') {
            when {
                expression { params.DEPLOY_TO_TOMCAT }
            }
            steps {
                sh '''
                    set -eu
                    WAR_FILE="$(find target -maxdepth 1 -type f -name '*.war' | head -n 1)"
                    if [ -z "$WAR_FILE" ]; then
                        echo "ERROR: No WAR file found in target/"
                        exit 1
                    fi

                    if [ ! -d "$TOMCAT_WEBAPPS_DIR" ]; then
                        echo "ERROR: Tomcat webapps directory not found: $TOMCAT_WEBAPPS_DIR"
                        echo "Install Tomcat on this agent or update TOMCAT_WEBAPPS_DIR in the Jenkinsfile."
                        exit 1
                    fi

                    if [ ! -w "$TOMCAT_WEBAPPS_DIR" ]; then
                        echo "ERROR: Jenkins agent user cannot write to $TOMCAT_WEBAPPS_DIR"
                        echo "Grant least-privilege write access to the agent user; avoid running Jenkins as root."
                        exit 1
                    fi

                    cp "$WAR_FILE" "$TOMCAT_WEBAPPS_DIR/${APP_NAME}.war"
                    echo "Copied ${WAR_FILE} to ${TOMCAT_WEBAPPS_DIR}/${APP_NAME}.war"
                    echo "Tomcat should deploy the application at /${APP_NAME}."
                    echo "Demo target label: ${DEPLOY_ENV}"
                '''
            }
        }
    }

    post {
        success {
            echo "SUCCESS: ${env.APP_NAME} pipeline completed."
            emailext (
                to: 'sharanappak111@gmail.com',
                subject: "Build Success: ${env.JOB_NAME}",
                body: "Great news! The build passed successfully."
            )
        }
        unstable {
            echo 'UNSTABLE: Review test reports or quality checks.'
        }
        failure {
            echo 'FAILURE: Open Console Output and find the first meaningful error.'
            emailext (
                to: 'sharanappak111@gmail.com',
                subject: "Build Failed: ${env.APP_NAME}",
                body: "Check the attached console logs for errors.",
                    )
        }
        aborted {
            echo 'ABORTED: The build was interrupted.'
        }
        always {
            echo "Finished build #${env.BUILD_NUMBER} for ${env.JOB_NAME}"
        }
    }
}
