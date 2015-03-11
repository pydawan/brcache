/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.brandao.brcache.client;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import org.brandao.brcache.server.ParameterException;
import org.brandao.brcache.server.ReadDataException;
import org.brandao.brcache.server.TerminalReader;
import org.brandao.brcache.server.TerminalWriter;
import org.brandao.brcache.server.TextTerminalReader;
import org.brandao.brcache.server.TextTerminalWriter;
import org.brandao.brcache.server.WriteDataException;

/**
 *
 * @author Cliente
 */
public class BrCacheConnection {
    
    public static final String CRLF     = "\r\n";
    
    public static final String BOUNDARY = "end";

    public static final String PUT      = "put";

    public static final String GET      = "get";
    
    public static final String REMOVE   = "remove";
    
    public static final String SUCCESS  = "ok";
    
    private String host;
    
    private int port;
    
    private Socket socket;
    
    private TerminalReader reader;
    
    private TerminalWriter writer;
    
    public BrCacheConnection(String host, int port){
        this.host = host;
        this.port = port;
    }
    
    public synchronized void connect() throws IOException{
        this.socket = new Socket(this.getHost(), this.getPort());
        this.reader = new TextTerminalReader(this.socket, 1*1024*1024);
        this.writer = new TextTerminalWriter(this.socket, 1*1024*1024);
    }

    public synchronized void disconect() throws IOException{
        
        if(this.socket != null)
            this.socket.close();
        
        this.reader = null;
        this.writer = null;
    }
    
    public synchronized void put(String key, long time, Object value) 
            throws WriteDataException, ReadDataException, ParameterException{
        this.writer.sendMessage(PUT);
        this.writer.sendMessage(key);
        this.writer.sendMessage(String.valueOf(time));
        
        ObjectOutputStream out = null;
        try{
            out = new ObjectOutputStream(this.writer.getStream());
            out.writeObject(value);
            out.flush();
        }
        catch(IOException ex){
            throw new WriteDataException("send entry fail: " + key, ex);
        }
        finally{
            if(out != null){
                try{
                    out.close();
                }
                catch(Exception ex){}
            }
            this.writer.sendCRLF();
            this.writer.sendMessage(BOUNDARY);
            this.writer.flush();
        }
        
        
        StringBuilder[] result = this.reader.getParameters(1);
        
        String resultSTR = result[0].toString();
        
        if(!resultSTR.equals(SUCCESS))
            throw new WriteDataException(resultSTR);
    }
    
    public synchronized Object get(String key) 
            throws WriteDataException, ReadDataException{
        this.writer.sendMessage(GET);
        this.writer.sendMessage(key);
        this.writer.flush();
        
        ObjectInputStream stream = null;
        try{
            stream = new ObjectInputStream(this.reader.getStream());
            return stream.readObject();
        }
        catch(EOFException ex){
            return null;
        }
        catch(IOException ex){
            throw new ReadDataException("read entry fail: " + key, ex);
        }
        catch(ClassNotFoundException ex){
            throw new ReadDataException("create instance fail: " + key, ex);
        }
        finally{
            if(stream != null){
                try{
                    stream.close();
                }
                catch(Exception e){}
            }
        }
    }

    public synchronized void remove(String key) throws WriteDataException, ReadDataException, ParameterException{
        this.writer.sendMessage(REMOVE);
        this.writer.sendMessage(key);
        this.writer.flush();
        
        StringBuilder[] response = this.reader.getParameters(1);
        
        if(!SUCCESS.equals(response.toString()))
            throw new WriteDataException(response.toString());
    }
    
    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
