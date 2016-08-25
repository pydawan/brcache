package org.brandao.brcache.tx;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.brandao.brcache.Cache;
import org.brandao.brcache.CacheErrors;
import org.brandao.brcache.CacheException;
import org.brandao.brcache.StreamCache;
import org.brandao.brcache.RecoverException;
import org.brandao.brcache.StorageException;
import org.brandao.brcache.SwaperStrategy;

public class TransactionInfo implements Serializable {

	private static final long serialVersionUID = 3758041685386590737L;

	private UUID id;
	
	private Set<String> updated;

	private Set<String> locked;
	
	private Set<String> managed;
	
	private Map<String, Long> times;
	
	private StreamCache entities;
	
	private Map<String, EntryCache> saved;
	
	private String path;
	
	public TransactionInfo(UUID id,
			long nodeBufferSize,
    		long nodePageSize,
    		double nodeSwapFactor,
    		
    		long indexBufferSize,
    		long indexPageSize,
    		double indexSwapFactor,
    		
    		long dataBufferSize,
    		long dataPageSize,
    		long blockSize,
    		double dataSwapFactor,
    		
    		long maxSizeEntry,
    		int maxSizeKey,
            SwaperStrategy swaperType,
			String path){
		this.id       = id;
		this.updated  = new HashSet<String>();
		this.locked   = new HashSet<String>();
		this.managed  = new HashSet<String>();
		this.times    = new HashMap<String, Long>();
		this.path     = path + "/" + id.toString();
		this.entities =	new Cache(
			nodeBufferSize, nodePageSize, nodeSwapFactor,
			indexBufferSize, indexPageSize, indexSwapFactor, 
			dataBufferSize, dataPageSize, blockSize, dataSwapFactor, 
			maxSizeEntry, maxSizeKey, this.path, swaperType, 1);
		this.entities.setDeleteOnExit(false);
		this.saved    = new HashMap<String, EntryCache>();
	}
	
	/* métodos de armazenamento */
	
	public Object replace(CacheTransactionManager manager, StreamCache cache,
			String key, Object value, long maxAliveTime, long time) throws StorageException {
		
		try{
			Object o = this.get(manager, cache, key, true, time);
			if(o != null){
				this.put(manager, cache, key, value, maxAliveTime, time);
				return true;
			}
			else
				return false;
		}
		catch(CacheException e){
			throw new StorageException(e, e.getError(), e.getParams());
		}
		catch(Throwable e){
			throw new StorageException(e, CacheErrors.ERROR_1020);
		}
	}
	
	public boolean replace(CacheTransactionManager manager, StreamCache cache,
			String key, Object oldValue, 
			Object newValue, long maxAliveTime, long time) throws StorageException {
		
		try{
			Object o = this.get(manager, cache, key, true, time);
			if(o != null && o.equals(oldValue)){
				this.put(manager, cache, key, newValue, maxAliveTime, time);
				return true;
			}
			else
				return false;
		}
		catch(CacheException e){
			throw new StorageException(e, e.getError(), e.getParams());
		}
		catch(Throwable e){
			throw new StorageException(e, CacheErrors.ERROR_1020);
		}
	}
	
	public Object putIfAbsent(CacheTransactionManager manager, StreamCache cache,
			String key, Object value, long maxAliveTime, long time) throws StorageException {
		
		try{
			Object o = this.get(manager, cache, key, true, time);
			
			if(o == null){
				this.put(manager, cache, key, value, maxAliveTime, time);
			}
			
			return o;
		}
		catch(CacheException e){
			throw new StorageException(e, e.getError(), e.getParams());
		}
		catch(Throwable e){
			throw new StorageException(e, CacheErrors.ERROR_1020);
		}
	}
	
	public void put(CacheTransactionManager manager, StreamCache cache,
			String key, Object value, long maxAliveTime, long time) throws StorageException {
		try{
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			ObjectOutputStream oout = new ObjectOutputStream(bout);
			oout.writeObject(value);
			oout.flush();
			oout.close();
			this.putStream(
				manager, cache, key, maxAliveTime, 
				new ByteArrayInputStream(bout.toByteArray()), time);
		}
		catch(CacheException e){
			throw new StorageException(e, e.getError(), e.getParams());
		}
		catch(Throwable e){
			throw new StorageException(e, CacheErrors.ERROR_1020);
		}
	}
	
    public void putStream(CacheTransactionManager manager, StreamCache cache, 
    		String key, long maxAliveTime, InputStream inputData, long time) 
    		throws StorageException {

    	try{
    		/*
			byte[] dta = 
					inputData == null? 
						null : 
						this.getBytes(inputData);
			*/
			this.lock(manager, key, time);
			this.entities.putStream(key, 0, inputData);
			this.managed.add(key);
			this.updated.add(key);
			this.times.put(key, maxAliveTime);
    	}
		catch(CacheException e){
			throw new StorageException(e, e.getError(), e.getParams());
		}
		catch(Throwable e){
			throw new StorageException(e, CacheErrors.ERROR_1020);
		}
    }
	
	/* métodos de coleta*/
	
	public Object get(CacheTransactionManager manager, StreamCache cache,
			String key, boolean forUpdate, long time) throws RecoverException {
		try{
			InputStream in = this.getStream(manager, cache, key, forUpdate, time);
			if(in != null){
				ObjectInputStream oin = new ObjectInputStream(in);
				return oin.readObject();
			}
			else
				return null;
		}
		catch(CacheException e){
			throw new StorageException(e, e.getError(), e.getParams());
		}
		catch(Throwable e){
			throw new StorageException(e, CacheErrors.ERROR_1021);
		}
	}
    
    public InputStream getStream(CacheTransactionManager manager, StreamCache cache, 
    		String key, boolean forUpdate, long time) throws RecoverException {
    	
    	try{
			InputStream dta = this.getEntity(manager, cache, key, forUpdate, time);
			return dta;
    	}
    	catch(RecoverException e){
    		throw e;
    	}
		catch(CacheException e){
			throw new StorageException(e, e.getError(), e.getParams());
		}
		catch(Throwable e){
			throw new StorageException(e, CacheErrors.ERROR_1021);
		}
    }

    /* métodos de remoção */
    
	public boolean remove(CacheTransactionManager manager, StreamCache cache,
			String key, Object value, long time) throws StorageException {
		
		try{
			Object o = this.get(manager, cache, key, true, time);
			if(o != null && o.equals(value)){
				return this.remove(manager, cache, key, time);
			}
			else
				return false;
		}
		catch(CacheException e){
			throw new StorageException(e, e.getError(), e.getParams());
		}
		catch(Throwable e){
			throw new StorageException(e, CacheErrors.ERROR_1021);
		}
	}
	
    public boolean remove(CacheTransactionManager manager, StreamCache cache,
    		String key, long time) throws StorageException{       
    	try{
			this.lock(manager, key, time);
			
    		if(this.managed.contains(key)){
    			this.updated.add(key);
    			return this.entities.remove(key);
    		}
    		else{
    			this.managed.add(key);
    			this.updated.add(key);
    			return cache.getStream(key) != null;
    		}
    	}
		catch(CacheException e){
			throw new StorageException(e, e.getError(), e.getParams());
		}
		catch(Throwable e){
			throw new StorageException(e, CacheErrors.ERROR_1021);
		}
    }
	
    /* métodos de manipulação*/
    
	public void savePoint(StreamCache cache) throws IOException, RecoverException{
		saved.clear();

		for(String key: this.updated){
			InputStream in = cache.getStream(key);
			if(in != null){
				saved.put(key, new EntryCache(this.getBytes(in), -1));
			}
			else{
				saved.put(key, null);
			}
		}
		
	}
    
	public void rollback(StreamCache cache) throws StorageException, RecoverException {
		
		for(String key: this.saved.keySet()){
			EntryCache entity = saved.get(key);
			if(entity == null){
				cache.remove(key);
			}
			else{
				cache.putStream(key, entity.getMaxAlive(), new ByteArrayInputStream(entity.getData()));
			}
		}
		
	}
	
	public void commit(StreamCache cache) throws RecoverException, StorageException {
		if(!this.updated.isEmpty()){
			for(String key: this.updated){
				InputStream entity = this.entities.getStream(key);
				
				if(entity == null){
					cache.remove(key);
				}
				else{
					long time = this.times.get(key);
					cache.putStream(key, time, entity);
				}
			}
			
		}
	}
	
	public void close() throws TransactionException{
		
		this.entities.destroy();
		this.locked.clear();
		this.managed.clear();
		this.saved.clear();
		this.times.clear();
		this.updated.clear();
		/*
		this.updated.clear();
		this.locked.clear();
		
		if(!this.managed.isEmpty()){
			for(String key: this.managed){
				this.entities.remove(key);
			}
		}
		
		this.times.clear();
		this.managed.clear();
		this.saved.clear();
		*/
	}
	
    /* métodos internos */
    
    private InputStream getEntity(CacheTransactionManager manager, StreamCache cache,
    		String key, boolean lock, long time) 
    		throws RecoverException, IOException, TransactionException{
    	
    	if(this.managed.contains(key)){
    		InputStream entry = this.entities.getStream(key);
    		return entry;
    	}
    	else{
    		InputStream dta = this.getSharedEntity(manager, cache, key, lock, time);
			this.managed.add(key);
			
			if(dta != null){
				this.entities.putStream(key, 0, dta);
				return this.entities.getStream(key);
			}
			else
				return dta;
    	}
    }
    
    private void lock(CacheTransactionManager manager, String key, long time) throws TransactionException{
    	
    	if(this.locked.contains(key)){
    		return;
    	}
    	
    	if(time <= 0){
    		manager.lock(this.id, key);
    	}
    	else{
			manager.tryLock(this.id, key, time, TimeUnit.MILLISECONDS);
    	}
    	
    	this.locked.add(key);
    }
    
    private InputStream getSharedEntity(CacheTransactionManager manager, StreamCache cache,
    		String key, boolean lock, long time) 
    		throws IOException, TransactionException, RecoverException{
    	
		if(lock){
			this.lock(manager, key, time);
		}
		
		InputStream in = cache.getStream(key);
		
		return in;
    }
    
    private byte[] getBytes(InputStream in) throws IOException {
    	ByteArrayOutputStream bout = new ByteArrayOutputStream(2048);
    	int l = 0;
    	byte[] buffer = new byte[2048];
    	
    	while((l = in.read(buffer, 0, buffer.length)) > 0){
    		bout.write(buffer, 0, l);
    	}
    	bout.close();
    	return bout.toByteArray();
    }
	
}
