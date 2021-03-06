/*
*  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
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

package org.wso2.carbon.identity.entitlement.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.caching.impl.CachingConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.entitlement.pdp.EntitlementEngine;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;

/**
 *
 */
public class EntitlementEngineCache {

    private static Log log = LogFactory.getLog(EntitlementEngineCache.class);
    public static final String ENTITLEMENT_ENGINE_CACHE_MANAGER = "ENTITLEMENT_ENGINE_CACHE_MANAGER";
    public static final String ENTITLEMENT_ENGINE_CACHE =
            CachingConstants.LOCAL_CACHE_PREFIX + "ENTITLEMENT_ENGINE_CACHE";
    private static final EntitlementEngineCache instance = new EntitlementEngineCache();

    /**
     * Gets a new instance of EntitlementEngineCache.
     *
     * @return A new instance of EntitlementEngineCache.
     */
    public static EntitlementEngineCache getInstance() {
        return instance;
    }


    private Cache<Integer, EntitlementEngine> getEntitlementCache() {
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            Cache<Integer, EntitlementEngine> cache;
            CacheManager cacheManager = Caching.getCacheManagerFactory().
                                                getCacheManager(ENTITLEMENT_ENGINE_CACHE_MANAGER);
            if (cacheManager != null) {
                cache = cacheManager.getCache(ENTITLEMENT_ENGINE_CACHE);
            } else {
                cache = Caching.getCacheManager().getCache(ENTITLEMENT_ENGINE_CACHE);
            }
            if (log.isDebugEnabled()) {
                log.debug("created authorization cache : " + cache);
            }
            return cache;
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    public EntitlementEngine get(int key){
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            EntitlementEngine entitlementEngine = getEntitlementCache().get(key);
            if(entitlementEngine != null){
                if(log.isDebugEnabled()){
                    log.debug("Cache : " + ENTITLEMENT_ENGINE_CACHE + "  is HIT " +
                            "for tenantId : " + key);
                }
                return entitlementEngine;
            }
            if(log.isDebugEnabled()){
                log.debug("Cache : " + ENTITLEMENT_ENGINE_CACHE + "  is MISSED " +
                        "for tenantId : " + key);
            }
            return entitlementEngine;
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    public void put(int key, EntitlementEngine engine){
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            getEntitlementCache().put(key, engine);
            if(log.isDebugEnabled()){
                log.debug("Cache : " + ENTITLEMENT_ENGINE_CACHE + " is populated with new entry " +
                        "with tenantId : " + key);
            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    public boolean contains(int key){
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            boolean contain = getEntitlementCache().containsKey(key);
            if(contain){
                if(log.isDebugEnabled()){
                    log.debug("Cache : " + ENTITLEMENT_ENGINE_CACHE + "  is HIT " +
                            "for tenantId : " + key);
                }
            } else {
                if(log.isDebugEnabled()){
                    log.debug("Cache : " + ENTITLEMENT_ENGINE_CACHE + "  is MISSED " +
                            "for tenantId : " + key);
                }
            }
            return contain;
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }
}
