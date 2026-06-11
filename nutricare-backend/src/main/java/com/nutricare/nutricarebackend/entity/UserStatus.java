package com.nutricare.nutricarebackend.entity;

public enum UserStatus {
    PENDING,
    PENDING_APPROVAL,
    PENDING_PROFILE,
    PENDING_SUBSCRIPTION,
    PROFILE_INCOMPLETE,
    SUBSCRIPTION_PENDING,
    APPROVED,
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    REJECTED,
    REMOVED,
    DELETED;

    public boolean isBlocked() {
        return this != ACTIVE;
    }
}
