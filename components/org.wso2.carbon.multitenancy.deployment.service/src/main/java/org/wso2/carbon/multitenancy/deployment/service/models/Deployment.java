package org.wso2.carbon.multitenancy.deployment.service.models;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Deployment model.
 */
@SuppressWarnings("unused")
@XmlRootElement
public class Deployment {

    private String id;
    private String product;
    private String version;
    private int pattern;

    /**
     * Deployment constructor
     * @param id deployment identifier
     * @param product product name
     * @param version product version
     * @param pattern deployment pattern
     */
    public Deployment(String id, String product, String version, int pattern) {
        this.id = id;
        this.product = product;
        this.version = version;
        this.pattern = pattern;
    }

    /**
     * Default constructor is required for XML marshalling.
     */
    public Deployment() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
}
