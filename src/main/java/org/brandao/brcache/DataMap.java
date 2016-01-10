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

package org.brandao.brcache;

import java.io.Serializable;

/**
 * 
 * @author Brandao
 */
public class DataMap implements Serializable{
    
	private static final long serialVersionUID = 3011721226561387998L;

	private long id;
    
    private long maxLiveTime;
    
    private int[] segments;

    private long length;
    
    private long crc;
    
    public long getMaxLiveTime() {
        return maxLiveTime;
    }

    public void setMaxLiveTime(long maxLiveTime) {
        this.maxLiveTime = maxLiveTime;
    }

    public int[] getSegments() {
        return segments;
    }

    public void setSegments(int[] segments) {
        this.segments = segments;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public long getCrc() {
        return crc;
    }

    public void setCrc(long crc) {
        this.crc = crc;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

}
