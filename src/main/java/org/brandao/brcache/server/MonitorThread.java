/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.brandao.brcache.server;

import org.brandao.brcache.Cache;

/**
 *
 * @author Cliente
 */
public class MonitorThread extends Thread{
    
    private boolean run;
    
    private Configuration config;
    
    private Cache cache;
    
    public MonitorThread(Cache cache, Configuration config){
        this.cache = cache;
        this.config = config;
    }
    
    public void start(){
        this.run = true;
        super.start();
    }

    public void kill(){
        this.run = false;
    }
    
    @Override
    public void run(){
        long lastRead = 0;
        long lastWrite = 0;
        long read = 0;
        long write = 0;
        while(this.run){
            try{
                Runtime runtime = Runtime.getRuntime();
                long memory = runtime.totalMemory() - runtime.freeMemory();
                this.config.setProperty("write_per_sec", String.valueOf(write-lastWrite));
                this.config.setProperty("read_per_sec", String.valueOf(read-lastRead));
                this.config.setProperty("total_memory", String.valueOf(runtime.totalMemory()));
                this.config.setProperty("used_memory", String.valueOf(memory));
                this.config.setProperty("used_memory", String.valueOf(memory));
                
                lastRead = cache.getCountRead();
                lastWrite = cache.getCountWrite();
                Thread.sleep(1000);
                read = cache.getCountRead();
                write = cache.getCountWrite();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    
}
