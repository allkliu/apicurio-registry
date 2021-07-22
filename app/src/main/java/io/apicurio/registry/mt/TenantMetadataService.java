/*
 * Copyright 2021 Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.apicurio.registry.mt;

import io.apicurio.multitenant.api.beans.RegistryTenantList;
import io.apicurio.multitenant.api.beans.SortBy;
import io.apicurio.multitenant.api.beans.SortOrder;
import io.apicurio.multitenant.api.beans.TenantStatusValue;
import io.apicurio.multitenant.api.datamodel.RegistryTenant;
import io.apicurio.multitenant.api.datamodel.UpdateRegistryTenantRequest;
import io.apicurio.multitenant.client.TenantManagerClient;
import io.apicurio.multitenant.client.exception.RegistryTenantNotFoundException;
import io.apicurio.registry.utils.Maybe;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Fabian Martinez
 * @author Jakub Senko <jsenko@redhat.com>
 */
@ApplicationScoped
public class TenantMetadataService {

    @Inject
    Maybe<TenantManagerClient> tenantManagerClient;

    //TODO create a TenantConfiguration object and only allow the access to it via the tenant context
    //TODO load the TenantConfiguration into the tenant context in the TenantIdResolver(maybe rename that class)
    //TODO cache the TenantConfiguration in TenantIdResolver
    public RegistryTenant getTenant(String tenantId) throws TenantNotFoundException {
        if (tenantManagerClient.isEmpty()) {
            throw new UnsupportedOperationException("Multitenancy is not enabled");
        }
        try {
            return tenantManagerClient.get().getTenant(tenantId);
        } catch (RegistryTenantNotFoundException e) {
            throw new TenantNotFoundException(e.getMessage());
        }
    }

    public List<RegistryTenant> getTenantsForDeletion() {
        if (tenantManagerClient.isEmpty()) {
            throw new UnsupportedOperationException("Multitenancy is not enabled");
        }
        final List<RegistryTenant> res = new ArrayList();
        final int limit = 50;
        int offset = 0;
        RegistryTenantList tenants = null;
        do {
            tenants = tenantManagerClient.get().listTenants(
                TenantStatusValue.TO_BE_DELETED,
                offset, limit, SortOrder.asc, SortBy.tenantId);
            res.addAll(tenants.getItems());
            offset += limit;
        } while (tenants.getItems().size() == limit);
        return res;
    }

    public void markTenantAsDeleted(String tenantId) {
        if (tenantManagerClient.isEmpty()) { // TODO Maybe unnecessary
            throw new UnsupportedOperationException("Multitenancy is not enabled");
        }
        UpdateRegistryTenantRequest ureq = new UpdateRegistryTenantRequest();
        ureq.setStatus(TenantStatusValue.DELETED);
        tenantManagerClient.get().updateTenant(tenantId, ureq);
    }
}
