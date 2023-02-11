package com.purplerat.prencryptor.tools;

import androidx.annotation.NonNull;


import com.purplerat.prencryptor.BuildConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PLog{
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private final Logger LOGGER;
    public <T>PLog(@NonNull Class<T> clazz){
        LOGGER = LoggerFactory.getLogger(clazz);
    }
    public void debug(String msg){
        if(DEBUG)LOGGER.debug(msg);
    }
    public void error(Exception e){
        if(DEBUG)LOGGER.error("",e);
    }
    public void error(String msg){
        if(DEBUG)LOGGER.error(msg);
    }
}
