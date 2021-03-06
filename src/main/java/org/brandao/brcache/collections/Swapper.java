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

import java.io.Serializable;

import org.brandao.brcache.Cache;

/**
 * Permite o envio e recebimento de entidades de outro nível. 
 * Por exemplo, as entidades podem ser enviadas para o disco ou outro cache.
 * 
 * @author Brandao
 */
public interface Swapper extends Serializable, Cloneable {
    
    /**
     * Define a identificação do agrupamento de entidades.
     * @param value identificação do agrupamento.
     */
    void setId(String value);
    
    /**
     * Envia uma entidade para o agrupamento.
     * @param index índice da entidade.
     * @param item Item.
     */
    void sendItem(long index, Entry<?> item);

    /**
     * Recupera uma entidade do agrupamento.
     * @param index índice
     * @return item.
     */
    Entry<?> getItem(long index);
    
    /**
     * Remove todos os agrupamentos.
     */
    void clear();
    
    /**
     * Destrói todos as agrupamentos. Executado quando a instâcnia do 
     * {@link Cache} associado a ele é destruido. 
     */
    void destroy();

    Swapper clone();
    
}
