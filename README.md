Portfolio Deployment Pipeline
This repository contains the Jenkins pipeline configurations for deploying a static portfolio website using Docker and Kubernetes (K3s).

Prerequisites
Before using this pipeline, ensure you have the following set up:

Jenkins installed with necessary plugins.
K3s cluster running.
Docker installed on the Jenkins server.
SonarQube server running for code quality analysis.
OWASP Dependency Check configured in Jenkins.
Access to a Docker Hub account.
Directory Structure
arduino
Copy code
/home/batman/DevOps-projects/K3s/Portfolio/
├── deployments/
│   └── portfolio-deployment.yaml
└── services/
    └── portfolio-service.yaml
    
Jenkins Pipeline Overview
The Jenkins pipeline consists of two main pipelines:

Continous Integration (CI) Pipeline
Continuous Deployment (CD) Pipeline

Continous Integration (CI) Pipeline

pipeline {
    agent any
    
    tools {
        jdk 'jdk17'  // JDK if needed for other tasks
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

        stage('SonarQube Analysis') {
            steps {
                sh '''$SCANNER_HOME/bin/sonar-scanner \
                      -Dsonar.projectKey=portfolio-website \
                      -Dsonar.sources=. \
                      -Dsonar.projectName=portfolio-website \
                      -Dsonar.host.url=http://localhost:9000 \
                      -Dsonar.login=<your sonar token>'''
            }
        }
        
        stage('OWASP Dependency Check') {
            steps {
                script {
                    dependencyCheck additionalArguments: "--project 'Portfolio' --scan . --nvdApiKey <your nvd api key>",
                                    odcInstallation: 'DependencyCheck'
                    dependencyCheckPublisher pattern: '**/dependency-check-report.xml'
                }
            }
        }
        
        stage('Build & Push Docker Image') {
            steps {
                script {
                    withDockerRegistry(credentialsId: 'Dockerhub', toolName: 'docker') {
                        sh 'ls -la'
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

Continuous Deployment (CD) Pipeline

pipeline {
    agent any

    stages {
        stage('Test Access') {
            steps {
                script {
                    sh 'ls -la /home/batman/DevOps-projects/K3s/Portfolio/deployments'
                }
            }
        }

        stage('Docker Deploy To K3s') {
            steps {
                script {
                    withDockerRegistry(credentialsId: 'Dockerhub', toolName: 'docker') {
                        sh "kubectl apply -f /home/batman/DevOps-projects/K3s/Portfolio/deployments"
                        sh "kubectl apply -f /home/batman/DevOps-projects/K3s/Portfolio/services"
                        sh "kubectl get pods -l app=portfolio"
                    }
                }
            }
        }
    }
}

Configuration Changes for Jenkins

To ensure the Jenkins pipeline can access the necessary directories and Kubernetes configurations, the following changes were made:

Created and configured .kube directory for the Jenkins user:


sudo mkdir -p /var/lib/jenkins/.kube
sudo chown -R jenkins:jenkins /var/lib/jenkins/.kube

Created a new group devops and added batman and jenkins users to it:

sudo groupadd devops
sudo usermod -aG devops batman
sudo usermod -aG devops jenkins

Changed ownership and permissions for deployment and service directories:

sudo chown -R batman:devops /home/batman/DevOps-projects/K3s/Portfolio/deployments
sudo chown -R batman:devops /home/batman/DevOps-projects/K3s/Portfolio/services/
sudo chmod -R 770 /home/batman/DevOps-projects/K3s/Portfolio/deployments
sudo chmod -R 770 /home/batman/DevOps-projects/K3s/Portfolio/services/

Created a cluster role binding for the Jenkins user:

kubectl create clusterrolebinding jenkins-admin-binding --clusterrole=cluster-admin --user=jenkins

Conclusion
This pipeline allows for efficient deployment of the portfolio website with continuous integration and deployment capabilities, ensuring code quality and security checks through SonarQube and OWASP Dependency Check.

