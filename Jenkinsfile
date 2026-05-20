pipeline {
    agent any

    environment {
        clde_branch_master = "master"
        clde_branch_staging = "staging"
        gitCredentialId = "github jenkins access token"
        awsCredentialsID = "4387aab6-ff4e-44e9-9e15-eb1452a3870b"
    }

    tools {
        maven 'M3'
        jdk 'openjdk11'
    }

    options {
        buildDiscarder(
            logRotator(artifactNumToKeepStr: '3', numToKeepStr: '5')
        )
        disableConcurrentBuilds()
    }

    stages {
        stage('Set Repository URLs') {
            steps {
                script {
                    def suffix = (env.BRANCH_NAME == clde_branch_master) ? 'master' : 'staging'
                    def base = "${WORKSPACE}/.."
                    def cldeRoot = "${base}/clde-server_${suffix}-cloudempiere/iDempiereCLDE"

                    env.IDEMPIERE_CORE_REPO = "file://${cldeRoot}/core/org.idempiere.p2/target/repository"
                    env.CLOUDEMPIERE_COMPOSITE_REPO = "file://${cldeRoot}/_composite/com.cloudempiere.composite.p2/target/repository"

                    echo "Repository URLs:"
                    echo "  iDempiere core:         ${env.IDEMPIERE_CORE_REPO}"
                    echo "  Cloudempiere composite: ${env.CLOUDEMPIERE_COMPOSITE_REPO}"
                }
            }
        }

        stage('Build') {
            when {
                anyOf {
                    branch "${clde_branch_staging}"
                    branch "${clde_branch_master}"
                }
            }
            steps {
                sh """
                    mvn clean verify -U \
                        -Didempiere.core.repository.url=${IDEMPIERE_CORE_REPO} \
                        -Dcloudempiere.composite.repository.url=${CLOUDEMPIERE_COMPOSITE_REPO}
                """
            }
        }

        stage('Publish Prod') {
            when {
                allOf {
                    branch "${clde_branch_master}"
                }
            }
            steps {
                withAWS(credentials: "${awsCredentialsID}", region: 'eu-west-1') {
                    s3Upload(
                        file: "${WORKSPACE}/com.cloudempiere.searchindex.p2/target/com.cloudempiere.searchindex.p2-10.2.0-SNAPSHOT.zip",
                        bucket: 'cloudempiere-releases',
                        path: "cloudempiere/production/com.cloudempiere.searchindex.p2-10.2.0-SNAPSHOT.zip"
                    )
                }
            }
        }

        stage('Publish Staging') {
            when {
                allOf {
                    branch "${clde_branch_staging}"
                }
            }
            steps {
                withAWS(credentials: "${awsCredentialsID}", region: 'eu-west-1') {
                    s3Upload(
                        file: "${WORKSPACE}/com.cloudempiere.searchindex.p2/target/com.cloudempiere.searchindex.p2-10.2.0-SNAPSHOT.zip",
                        bucket: 'cloudempiere-releases',
                        path: "cloudempiere/development/com.cloudempiere.searchindex.p2-10.2.0-SNAPSHOT.zip"
                    )
                }
            }
        }
    }
}
