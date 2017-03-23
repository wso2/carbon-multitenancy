/*
 * Copyright (c) 2017, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

    @Override
    public String toString() {
        return String.format("id: %s product: %s version: %s pattern: %d", getId(), getProduct(), getVersion(),
                getPattern());
    }
}
