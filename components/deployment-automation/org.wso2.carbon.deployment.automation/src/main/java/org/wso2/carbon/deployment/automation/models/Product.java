package org.wso2.carbon.deployment.automation.models;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Product model.
 */
@SuppressWarnings("unused")
@XmlRootElement
public class Product {
    private String product;
    private String version;
    private int pattern;
    private String platform;

    /**
     * Default constructor is required for XML marshalling.
     */
    public Product() {
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getPattern() {
        return pattern;
    }

    public void setPattern(int pattern) {
        this.pattern = pattern;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }
}
