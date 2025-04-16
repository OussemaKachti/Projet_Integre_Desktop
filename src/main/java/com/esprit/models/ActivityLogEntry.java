package com.esprit.models;

import java.time.LocalDateTime;

/**
 * Model class representing an activity log entry for the administrator.
 * This aggregates activity data from multiple tables in the database.
 */
public class ActivityLogEntry {
    private LocalDateTime dateTime;
    private String activityType;
    private String description;
    private String ipAddress;
    private int entityId; // ID of the related entity (user, club, event, etc.)
    private String entityType; // Type of the entity (user, club, event, etc.)
    
    /**
     * Default constructor
     */
    public ActivityLogEntry() {
    }
    
    /**
     * Constructor with all required fields
     */
    public ActivityLogEntry(LocalDateTime dateTime, String activityType, String description, 
                           String ipAddress, int entityId, String entityType) {
        this.dateTime = dateTime;
        this.activityType = activityType;
        this.description = description;
        this.ipAddress = ipAddress;
        this.entityId = entityId;
        this.entityType = entityType;
    }
    
    /**
     * Constructor without entity details for backward compatibility
     */
    public ActivityLogEntry(LocalDateTime dateTime, String activityType, String description, String ipAddress) {
        this.dateTime = dateTime;
        this.activityType = activityType;
        this.description = description;
        this.ipAddress = ipAddress;
        this.entityId = 0;
        this.entityType = "None";
    }
    
    // Getters and Setters
    
    public LocalDateTime getDateTime() {
        return dateTime;
    }
    
    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }
    
    public String getActivityType() {
        return activityType;
    }
    
    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public int getEntityId() {
        return entityId;
    }
    
    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
    
    @Override
    public String toString() {
        return "ActivityLogEntry{" +
                "dateTime=" + dateTime +
                ", activityType='" + activityType + '\'' +
                ", description='" + description + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", entityId=" + entityId +
                ", entityType='" + entityType + '\'' +
                '}';
    }
} 