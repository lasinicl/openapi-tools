/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.openapi.core.generators.client;

import io.ballerina.openapi.core.model.Filter;
import io.swagger.v3.oas.models.OpenAPI;

/**
 * This class stores metadata that related to client code generations.
 *
 * @since 1.3.0
 */
public class ClientMetaData {
    private final OpenAPI openAPI;
    private final Filter filters;
    private final boolean nullable;
    private final boolean resourceMode;
    private final boolean isPlugin;

    public ClientMetaData(ClientMetaDataBuilder clientMetaDataBuilder) {
        this.openAPI = clientMetaDataBuilder.openAPI;
        this.filters = clientMetaDataBuilder.filters;
        this.nullable = clientMetaDataBuilder.nullable;
        this.isPlugin = clientMetaDataBuilder.isPlugin;
        this.resourceMode = clientMetaDataBuilder.resourceMode;
    }

    public OpenAPI getOpenAPI() {
        return openAPI;
    }

    public Filter getFilters() {
        return filters;
    }

    public boolean isNullable() {
        return nullable;
    }

    public boolean isResourceMode() {
        return resourceMode;
    }

    public boolean isPlugin() {
        return isPlugin;
    }

    /**
     * Client IDL plugin meta data builder class.
     */
    public static class ClientMetaDataBuilder {
        private OpenAPI openAPI;
        private Filter filters;
        private boolean nullable = false;
        private boolean resourceMode = true;
        private boolean isPlugin = false;

        public ClientMetaDataBuilder withOpenAPI(OpenAPI openAPI) {
            this.openAPI = openAPI;
            return this;
        }

        public ClientMetaDataBuilder withFilters(Filter filters) {
            this.filters = filters;
            return this;
        }

        public ClientMetaDataBuilder withNullable(boolean nullable) {
            this.nullable = nullable;
            return this;
        }

        public ClientMetaDataBuilder withResourceMode(boolean resourceMode) {
            this.resourceMode = resourceMode;
            return this;
        }

        public ClientMetaDataBuilder withPlugin(boolean isPlugin) {
            this.isPlugin = isPlugin;
            return this;
        }

        public ClientMetaData build() {
            return new ClientMetaData(this);
        }
    }
}
