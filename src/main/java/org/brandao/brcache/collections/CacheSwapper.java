/*
 * BRCache http://brcache.brandao.org/
 * Copyright (C) 2015 Afonso Brandao. (afonso.rbn@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.brandao.brcache.collections;

import org.brandao.brcache.Cache;
import org.brandao.brcache.RecoverException;
import org.brandao.brcache.StorageException;
import org.brandao.brcache.client.BrCacheClient;

/**
 *
 * @author Brandao
 */
public class CacheSwapper<T> implements Swapper<T>{

    private String id;
    
    private long maxalive;
    
    private Integer maxIndex;
    
    public CacheSwapper(){
        this.maxIndex = -1;
    }
    
    public void setPath(String value) {
    }

    public void setId(String value) {
        this.id = value;
    }

    public void persistDiskItem(Integer index, Entry<T> item) {
        synchronized(this){
            if(this.maxIndex < index)
                this.maxIndex = index;
        }
        
        try{
            if(CacheList.getCache() != null)
                CacheList.getCache().putObject(new String(this.id + ":" + index), this.getMaxalive(), item);
            else
                CacheList.getClient().put(new String(this.id + ":" + index), this.getMaxalive(), item);

        }
        catch (StorageException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Entry<T> readDiskItem(Integer index) {
        try{
            if(CacheList.getCache() != null)
                return (Entry<T>) CacheList.getCache().getObject(new String(this.id + ":" + index));
            else
                return (Entry<T>) CacheList.getClient().get(new String(this.id + ":" + index));
                
        }
        catch (RecoverException ex) {
            throw new RuntimeException(ex);
        }
    }

    public long getMaxalive() {
        return maxalive;
    }

    public void setMaxalive(long maxalive) {
        this.maxalive = maxalive;
    }

    public void clear() {
        try{
            if(CacheList.getCache() != null){
                Cache cache = CacheList.getCache();
                for(int i=0;i<=this.maxIndex;i++){
                    cache.remove(new String(this.id + ":" + i));
                }
            }
            else{
                BrCacheClient client = CacheList.getClient();
                for(int i=0;i<=this.maxIndex;i++){
                    client.remove(new String(this.id + ":" + i));
                }
            }
                
        }
        catch (RecoverException ex) {
            throw new RuntimeException(ex);
        }
    }
    
}
