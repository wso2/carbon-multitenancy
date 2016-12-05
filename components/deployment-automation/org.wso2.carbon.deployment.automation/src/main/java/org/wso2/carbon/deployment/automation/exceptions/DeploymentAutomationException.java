package org.wso2.carbon.deployment.automation.exceptions;

/**
 * Deployment automation exception.
 */
public class DeploymentAutomationException extends Exception {

    public DeploymentAutomationException(String message) {
        super(message);
    }

    public DeploymentAutomationException(String message, Throwable cause) {
        super(message, cause);
    }
}
