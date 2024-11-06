pipeline {
    agent any

    stages {
        stage('Test Access') {
            steps {
                script {
                    // Check if Jenkins can access the deployments directory
                    sh 'ls -la /home/batman/DevOps-projects/K3s/Portfolio/deployments'
                }
            }
        }

        stage('Docker Deploy To K3s') {
            steps {
                script {
                    withDockerRegistry(credentialsId: 'Dockerhub', toolName: 'docker') {
                        // Apply Kubernetes deployment and service YAMLs
                        sh "kubectl apply -f /home/batman/DevOps-projects/K3s/Portfolio/deployments"
                        sh "kubectl apply -f /home/batman/DevOps-projects/K3s/Portfolio/services"

                        // Optionally, confirm that the pods are running
                        sh "kubectl get pods -l app=portfolio"

                        // Rollout restart to apply the new changes
                        sh "kubectl rollout restart deployment portfolio-deployment"

                        // Verify the status of the deployment
                        sh "kubectl get deployment portfolio-deployment"
                    }
                }
            }
        }
    }
}

