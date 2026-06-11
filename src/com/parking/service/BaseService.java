package com.parking.service;

import com.parking.storage.DataStore;

public abstract class BaseService {

    protected final DataStore dataStore;

    protected BaseService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public abstract String getServiceName();

    protected String log(String message) {
        return "[" + getServiceName() + "] " + message;
    }
}
