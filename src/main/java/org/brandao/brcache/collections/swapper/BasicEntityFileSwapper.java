package org.brandao.brcache.collections.swapper;

import org.brandao.brcache.CacheException;
import org.brandao.brcache.collections.Entry;
import org.brandao.brcache.collections.fileswapper.AbstractEntityFileSwapper;
import org.brandao.entityfilemanager.EntityFileManager;

public class BasicEntityFileSwapper<T> 
	extends AbstractEntityFileSwapper<T>{

	private static final long serialVersionUID = 4746825276737497673L;

	private EntityFileManager efm;

	private String name;
	
	private volatile long maxID;
	
	@SuppressWarnings("unchecked")
	public BasicEntityFileSwapper(EntityFileManager efm, String name, Class<?> type){
		this.efm   = efm;
		this.name  = name;
		this.type  = (Class<T>) type;
	}
	
	public void sendItem(long index, Entry<T> item) throws CacheException{
		if(maxID <= index){
			super.allocSpace(efm, name, index, item);
		}
		else{
			super.update(efm, name, index, item);
		}
	}
	
	public Entry<T> getItem(long index) throws CacheException{
		Entry<T> e = super.get(efm, name, index);
		if(e != null){
			e.setNeedUpdate(false);
		}
		return e;
	}

	public synchronized void clear() throws CacheException{
	}

	public synchronized void destroy() throws CacheException{
	}

}