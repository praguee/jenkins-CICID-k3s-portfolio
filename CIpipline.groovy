pipeline {
    agent any
    
    tools {
        jdk 'jdk17'  // JDK if needed for other tasks
        // Removed Maven since it's not needed for static websites
    }
    
    environment {
        SCANNER_HOME = tool 'sonar-scanner'
    }
    
    stages {
        stage('Git Checkout') {
            steps {
                git changelog: false, credentialsId: 'github', poll: false, url: 'https://github.com/praguee/containerizing-portfolio.git'
            }
        }
        
        // No compile stage since this is a static website

        stage('SonarQube Analysis') {
            steps {
                sh '''$SCANNER_HOME/bin/sonar-scanner \
                      -Dsonar.projectKey=portfolio-website \
                      -Dsonar.sources=. \
                      -Dsonar.projectName=portfolio-website \
                      -Dsonar.host.url=http://localhost:9000 \
                      -Dsonar.login=squ_cfb10b8b90d67ba2c50198c8fe7becb4d92436f7'''
            }
        }
        
        stage('OWASP Dependency Check') {
            steps {
                script {
                    // Running OWASP Dependency Check with NVD API Key
                    dependencyCheck additionalArguments: "--project 'Portfolio' --scan . --nvdApiKey d8764adb-3b45-4c17-98e2-494f431cb909 --noupdate -f HTML -f XML",
                                    odcInstallation: 'DependencyCheck'
                    
                    // Publish the generated OWASP Dependency Check report
                    dependencyCheckPublisher pattern: '**/dependency-check-report.xml'
                }
            }
        }
        
        stage('Build & Push Docker Image') {
            steps {
                script {
                    withDockerRegistry(credentialsId: 'Dockerhub', toolName: 'docker') {
                        // List workspace content for debugging
                        sh 'ls -la'
                        
                        // Update the Dockerfile path if it's not in the docker directory
                        sh "docker build -t portfolio:latest -f Dockerfile ."
                        sh "docker tag portfolio:latest prrague/portfolio:latest"
                        sh "docker push prrague/portfolio:latest"
                    }
                }
            }
        }
        
        stage('Trigger CD Pipeline') {
            steps {
                build job: "CD pipeline", wait: true
            }
        }
    }
}
