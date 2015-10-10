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

package org.brandao.brcache.ncache;

import org.brandao.brcache.Cache;
import org.brandao.brcache.DataMap;
import org.brandao.brcache.RecoverException;
import org.brandao.brcache.StorageException;
import org.brandao.brcache.SwaperStrategy;
import org.brandao.brcache.collections.StringTreeKey;
import org.brandao.brcache.collections.TreeHugeMap;
import org.brandao.brcache.collections.TreeKey;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.brandao.brcache.collections.Collections;
import org.brandao.brcache.collections.DiskSwapper;
import org.brandao.brcache.collections.TreeFileSwaper;
import org.brandao.brcache.collections.Swapper;

/**
 * Representa um cache.
 * 
 * @author Brandao
 */
public class NCache extends Cache{
    
    private final TreeHugeMap<TreeKey,DataMap> dataMap;

    private final MemoryManager manager;
    
    private final int segmentSize;
    
    private final int writeBufferLength;

    private final int maxBytesToStorageEntry;
    
    private final int maxLengthKey;
    
    volatile long countRead;
    
    volatile long countWrite;
    
    volatile long countReadData;

    volatile long countWriteData;

    private String dataPath;
    
    /**
     * Cria um novo cache.
     * 
     * @param nodesSize Quantidade de bytes usados para armazenar os nós na memória.
     * @param nodesSwapSize Tamanho do bloco de swap dos nós.
     * @param nodesSwapFactor Fator de swap dos nós.
     * @param indexSize Quantidade de bytes usados para armazenar os índices dos itens na memória.
     * @param indexSwapSize Tamanho do bloco de swap dos índices.
     * @param indexSwapFactor Fator de swap dos índices.
     * @param dataSize Quantidade de bytes usados para armazenar os itens na memória.
     * @param dataSwapSize Tamanho do bloco de swap dos itens.
     * @param dataSwapFactor Fator de swap dos itens.
     * @param maxSlabSize Tamanho do agrupamento dos dados do itens em bytes.
     * @param writeBufferSize Tamanho do buffer de escrita no cache.
     * @param maxSizeEntry Tamanho máximo em bytes que um item pode ter para ser armazenado no cache.
     * @param maxSizeKey Tamanho máximo em bytes que uma chave pode ter.
     * @param dataPath Pasta onde os dados do cache serão armazenados no processo de swap.
     * @param swaperType Estratégia de swap.
     * @param lockFactor Fator de lock. Determina quantos locks serão usados para bloquear os segmentos.
     * @param quantitySwaperThread Quantidade de threads que irão fazer o swap.
     */
    public NCache(
        long nodesSize,
        long nodesSwapSize,
        double nodesSwapFactor,
        long indexSize,
        long indexSwapSize,
        double indexSwapFactor,
        long dataSize,
        long dataSwapSize,
        double dataSwapFactor,
        int maxSlabSize,
        int writeBufferSize,
        int maxSizeEntry,
        int maxSizeKey,
        String dataPath,
        SwaperStrategy swaperType,
        double lockFactor,
        int quantitySwaperThread){
        
        if(nodesSwapSize > nodesSize)
            throw new RuntimeException("nodesSwap_size > nodesSize");

        if(indexSwapSize > indexSwapSize)
            throw new RuntimeException("indexSwapSize > indexSwapSize");

        if(maxSlabSize > dataSwapSize)
            throw new RuntimeException("maxSlabSize > dataSwapSize");

        if(dataSwapSize/maxSlabSize < 1.0)
            throw new RuntimeException("dataSwapSize must be greater than " + maxSlabSize);

        if(dataSwapSize > dataSize)
            throw new RuntimeException("dataSwapSize > dataSize");

        if(lockFactor < 0)
            throw new RuntimeException("quantityLock < 0.0");
        
        if(quantitySwaperThread < 1)
            throw new RuntimeException("quantitySwaperThread < 1");
            
        double nodesOnMemory          = nodesSize/8.0;
        double nodesPerSegment        = nodesSwapSize/8.0;
        double swapSegmentNodesFactor = nodesSwapFactor;
        
        double indexOnMemory          = indexSize/40.0;
        double indexPerSegment        = indexSwapSize/40.0;
        double swapSegmentIndexFactor = indexSwapFactor;
        
        double bytesOnMemory          = dataSize/maxSlabSize;
        double bytesPerSegment        = dataSwapSize/maxSlabSize;
        double swapSegmentsFactor     = dataSwapFactor;
        
        this.dataPath               = dataPath;
        this.segmentSize            = maxSlabSize;
        this.writeBufferLength      = writeBufferSize;
        this.maxBytesToStorageEntry = maxSizeEntry;
        this.maxLengthKey           = maxSizeKey;
        
        synchronized(Collections.class){
            this.dataMap =
                    new TreeHugeMap<TreeKey, DataMap>(
                    "dataMap",
                    (int)nodesOnMemory,
                    swapSegmentNodesFactor,
                    nodesPerSegment/nodesOnMemory,
                    this.getSwaper(swaperType),
                    (int)((nodesOnMemory/nodesPerSegment)*lockFactor) + 1,
                    quantitySwaperThread,
                    (int)indexOnMemory,
                    swapSegmentIndexFactor,
                    indexPerSegment/indexOnMemory,
                    this.getSwaper(swaperType),
                    (int)((indexOnMemory/indexPerSegment)*lockFactor) + 1,
                    quantitySwaperThread
                    );

            this.manager = null;
            //this.manager = 
        	//	new MemoryManager(maxSlabSize, 
        	//			dataSize, dataSwapSize, this.getSwaper(swaperType));
        }   
    }
    
    /**
     * Cria um novo cache.
     * 
     */
    public NCache(){
        this(
        28*1024,   //28kb
        16*1024,      //16kb
        0.3,
        28*1024,   //28kb
        16*1024,      // 16kb
        0.3,
        60*1024*1024,  //512mb
        1*1024*1024,    //1mb
        0.6,
        16*1024,
        8012,
        1048576,     //1mb
        128,
        "/mnt/brcache",
        SwaperStrategy.FILE_TREE,
        0.1,
        1);
    }
    
    /**
     * Obtém a estratégia de swap dos dados do cache.
     * 
     * @param strategy Tipo da estratégia.
     * @return Estratégia.
     */
    protected Swapper getSwaper(SwaperStrategy strategy){
        Swapper swapper = new TreeFileSwaper();
        
        if(swapper instanceof DiskSwapper)
            ((DiskSwapper)swapper).setRootPath(this.dataPath);
        
        return swapper;
    }
    
    /**
     * Inclui ou sobrescreve um objeto no cache.
     * 
     * @param key Identificação do objeto no cache.
     * @param maxAliveTime Tempo máximo em milesegundos que o objeto ficará no cache.
     * @param item Objeto a ser incluído no cache.
     * @throws StorageException Lançada se ocorrer alguma falha ao tentar inserir o
     * objeto no cache.
     */
    public void putObject(String key, long maxAliveTime, Object item) throws StorageException{
        try{
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream oout = new ObjectOutputStream(bout);
            oout.writeObject(item);
            oout.flush();
            this.put(key, maxAliveTime, new ByteArrayInputStream(bout.toByteArray()));
        }
        catch(StorageException e){
            throw e;
        }
        catch(Throwable e){
            throw new StorageException(e);
        }
        
    }

    /**
     * Recupera um objeto do cache.
     * 
     * @param key Identificação do objeto no cache.
     * @return Objeto ou <code>null</code>.
     * @throws RecoverException Lançada se ocorrer alguma falha ao tentar recuperar o
     * objeto do cache.
     */
    public Object getObject(String key) throws RecoverException{
        try{
            InputStream in = this.get(key);
            if(in != null){
                ObjectInputStream oin = new ObjectInputStream(in);
                return oin.readObject();
            }
            else
                return null;
        }
        catch(Throwable e){
            throw new RecoverException(e);
        }
        
    }

    /**
     * Inclui ou sobrescreve um item no cache.
     * 
     * @param key Identificação do item no cache.
     * @param maxAliveTime Tempo máximo em milesegundos que o item ficará no cache.
     * @param inputData Fluxo de dados que representa o item.
     * @throws StorageException Lançada se ocorrer alguma falha ao tentar inserir o
     * item no cache.
     */
    public void put(String key, long maxAliveTime, InputStream inputData) throws StorageException{
        
        if(key.length() > this.maxLengthKey)
            throw new StorageException("key is very large");
        
        TreeKey treeKey = new StringTreeKey(key);
        DataMap map     = new DataMap();
        map.setMaxLiveTime(maxAliveTime);
        this.putData(map, inputData);
        this.dataMap.put(treeKey, map);
        this.countWriteData += map.getLength();
        this.countWrite++;
    }

    /**
     * Recupera um item do cache.
     * 
     * @param key Identificação do item no cache.
     * @return Fluxo de dados que representa o item ou <code>null</code>.
     * @throws RecoverException Lançada se ocorrer alguma falha ao tentar recuperar o
     * item do cache.
     */
    public InputStream get(String key) throws RecoverException{
        
        try{
            countRead++;

            DataMap map = this.dataMap.get(new StringTreeKey(key));

            if(map != null)
                return new NCacheInputStream(this, map, this.manager);
            else
                return null;
        }
        catch(Throwable e){
            throw new RecoverException(e);
        }
    }
    
    /**
     * Remove um item do cache.
     * 
     * @param key Identificação do item no cache.
     * @return Verdadeiro se o item for removido. Caso contrário falso.
     * @throws RecoverException Lançada se ocorrer alguma falha ao tentar remover o
     * item do cache.
     */
    public boolean remove(String key) throws RecoverException{
        
        try{
            DataMap data = this.dataMap.get(new StringTreeKey(key));

            if(data != null){

                this.dataMap.put(new StringTreeKey(key), null);

                synchronized(this.manager){
                    int[] segments = data.getSegments();

                    for(int segment: segments){
                        this.manager.releaseSegment(segment);
                    }
                }
                return true;
            }
            else
                return false;
        }
        catch(Throwable e){
            throw new RecoverException(e);
        }
        
    }
    
    private void putData(DataMap map, InputStream inputData) throws StorageException{
        
    	try{
    		MemoryEntry memoryEntry = this.manager.write(inputData);
    		Integer[] tmp = memoryEntry.getSegments();
    		int[] segs = new int[tmp.length];
    		for(int i=0;i<segs.length;i++){
    			segs[i] = tmp[i];
    		}
    		map.setLength(memoryEntry.getSize());
    		map.setSegments(segs);
    	}
    	catch(Throwable e) {
    		throw new StorageException(e); 
		}
    }

    /**
     * Obtém a quantidade de item recuperados.
     * 
     * @return Quantidade de item recuperados.
     */
    public long getCountRead(){
        return this.countRead;
    }

    /**
     * Obtém a quantidade de item armazenados.
     * 
     * @return Quantidade de item armazenados.
     */
    public long getCountWrite(){
        return this.countWrite;
    }

    /**
     * Obtém a quantidade de bytes recuperados.
     * 
     * @return Quantidade de bytes recuperados.
     */
    public long getCountReadData() {
        return countReadData;
    }
    
    /**
     * Obtém a quantidade de bytes armazenados.
     * 
     * @return Quantidade de bytes armazenados.
     */
    public long getCountWriteData() {
        return countWriteData;
    }
    
}