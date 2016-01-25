
package org.brandao.brcache.client;

import java.io.FileNotFoundException;
import java.io.IOException;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.brandao.brcache.CacheException;
import org.brandao.brcache.server.Main;

/**
 *
 * @author Brandao
 */
public class BrCacheClientTest extends TestCase{
    
    public void test() 
            throws FileNotFoundException, IOException, ClassNotFoundException, InterruptedException, CacheException{
        
        Main server = new Main();
        server.main(new String[0]);
        /*
        Thread server = new Thread(){
            
            public void run(){
                try{
                    Main server = new Main();
                    server.main(new String[0]);
                }
                catch(Throwable e){
                    e.printStackTrace();
                }
            }
        };
        
        server.start();
        
        final BrCacheClient client = new BrCacheClient("localhost", 9090, 10, 20);
        client.connect();

        String text = "";
        for(int i=0;i<64000;i++){
            text += "A";
            client.put("AA", 0, text);
            String r = (String)client.get("AA");
            Assert.assertEquals(text,r);
        }
        */
    }        
    
    public void testInsertOnMemory() 
            throws FileNotFoundException, IOException, ClassNotFoundException, InterruptedException, CacheException{
        
        Thread server = new Thread(){
            
            public void run(){
                try{
                    Main server = new Main();
                    server.main(new String[0]);
                }
                catch(Throwable e){
                    e.printStackTrace();
                }
            }
        };
        
        server.start();
        final BrCacheClient client = new BrCacheClient("localhost", 9090, 10, 20);
        client.connect();

        Thread th = new Thread(new PutTask(client, 100));
        th.start();

        th = new Thread(new GetTask(client));
        th.start();
        
        Thread.sleep(999999999);
        
    }        
    
    public void testOverrideMemory() 
            throws FileNotFoundException, IOException, ClassNotFoundException, InterruptedException, CacheException{
        
        Thread server = new Thread(){
            
            public void run(){
                try{
                    Main server = new Main();
                    server.main(new String[0]);
                }
                catch(Throwable e){
                    e.printStackTrace();
                }
            }
        };
        
        server.start();
        
        final BrCacheClient client = new BrCacheClient("localhost", 9090, 1, 1);
        client.connect();

        PutTask o = new PutTask(client, 100);
        o.run();

        Thread th = new Thread(new OverrideTask(client));
        th.start();
        
        Thread.sleep(999999999);
        
    }        
    
    public void testPut() 
            throws FileNotFoundException, IOException, ClassNotFoundException, InterruptedException, CacheException{
        
        Thread server = new Thread(){
            
            public void run(){
                try{
                    Main server = new Main();
                    server.main(new String[0]);
                }
                catch(Throwable e){
                    e.printStackTrace();
                }
            }
        };
        
        server.start();
        
        final BrCacheClient client = new BrCacheClient("localhost", 9090, 10, 20);
        client.connect();

        for(int i=0;i<10;i++){
            Thread th = new Thread(new PutTask(client,-1));
            th.start();
        }

        for(int i=0;i<1;i++){
            Thread th = new Thread(new GetTask(client));
            th.start();
        }
        
        Thread.sleep(999999999);
        
    }    
    
}
