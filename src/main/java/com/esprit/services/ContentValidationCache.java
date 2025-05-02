package com.esprit.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.json.JSONObject;

/**
 * Cache for validation results to minimize API calls
 */
public class ContentValidationCache {
    private static final Logger LOGGER = Logger.getLogger(ContentValidationCache.class.getName());
    private static ContentValidationCache instance;
    
    private final Map<String, CachedResult> cache = new ConcurrentHashMap<>();
    
    private ContentValidationCache() {
        // Private constructor for singleton
    }
    
    public static synchronized ContentValidationCache getInstance() {
        if (instance == null) {
            instance = new ContentValidationCache();
        }
        return instance;
    }
    
    /**
     * Get a cached result
     * @param key Cache key
     * @return JSONObject result or null if not in cache or expired
     */
    public JSONObject get(String key) {
        CachedResult result = cache.get(key);
        
        if (result == null) {
            return null;
        }
        
        // Check if expired
        if (result.isExpired()) {
            LOGGER.info("Cache entry expired for key: " + key);
            cache.remove(key);
            return null;
        }
        
        LOGGER.info("Cache hit for key: " + key);
        return result.getValue();
    }
    
    /**
     * Put a result in the cache
     * @param key Cache key
     * @param value JSONObject result
     * @param duration Duration value
     * @param unit Duration time unit
     */
    public void put(String key, JSONObject value, long duration, TimeUnit unit) {
        long expiryTime = System.currentTimeMillis() + unit.toMillis(duration);
        cache.put(key, new CachedResult(value, expiryTime));
        LOGGER.info("Added to cache with key: " + key);
    }
    
    /**
     * Clear the entire cache
     */
    public void clear() {
        cache.clear();
        LOGGER.info("Cache cleared");
    }
    
    /**
     * Get the current size of the cache
     * @return Number of entries in the cache
     */
    public int size() {
        return cache.size();
    }
    
    /**
     * Inner class to represent a cached result with expiry time
     */
    private static class CachedResult {
        private final JSONObject value;
        private final long expiryTime;
        
        public CachedResult(JSONObject value, long expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }
        
        public JSONObject getValue() {
            return value;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
} 