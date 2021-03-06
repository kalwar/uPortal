/**
 * Licensed to Apereo under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Apereo licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.portal.groups;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * A composite key and type that uniquely identify a portal entity.  The composite
 * key contains a service name, which may be compound, and a native key, which is
 * the key that identifies the entity in the local service.
 *
 * @author Dan Ellentuck
 */
public class CompositeEntityIdentifier extends org.jasig.portal.EntityIdentifier 
implements IGroupConstants
{
    // static vars:
    protected static final String separator;
    private static final LoadingCache<String, Name> NAME_PARSE_CACHE = CacheBuilder.newBuilder().maximumSize(1000).build(new CacheLoader<String, Name>() {
        @Override
        public Name load(String s) throws Exception {
            int start = 0;
            int separatorLength = separator.length();
            int end = s.indexOf(separator, start);
            List<String> list = new ArrayList<String>(4);
            while (end != -1)
            {
                list.add(s.substring(start,end));
                start = end + separatorLength;
                end = s.indexOf(separator, start);
            }
            list.add(s.substring(start));
            return new CompositeEntityIdentifier.NameImpl(list);
        }
    });
    
    static {
        String sep;
        try 
            { sep = GroupServiceConfiguration.getConfiguration().getNodeSeparator(); }
        catch (Exception ex) 
            { sep = IGroupConstants.NODE_SEPARATOR; }
        separator = sep;
    }
    
    // instance vars:
    protected Name compositeKey;
    protected String cachedCompositeKey;
    protected String cachedLocalKey;
    protected Name cachedServiceName;
    
/**
 * @param entityKey java.lang.String
 * @param entityType java.lang.Class
 */
public CompositeEntityIdentifier(String entityKey, Class entityType) 
throws GroupsException
{
    super(entityKey, entityType);
    try
        { compositeKey = parseCompoundKey(entityKey); }
    catch (NamingException ne)
        { throw new GroupsException("Error in group key", ne);}
}
/**
 * @return javax.naming.Name
 */
protected Name getCompositeKey() 
{
    return compositeKey;
}
/**
 * @return java.lang.String
 */
public synchronized String getKey() {
    if ( cachedCompositeKey == null ) 
        { cachedCompositeKey = getCompositeKey().toString(); }
    return cachedCompositeKey;
}
/**
 * @return java.lang.String
 */
public synchronized String getLocalKey() {
    if ( cachedLocalKey == null )
        { cachedLocalKey = getCompositeKey().get(size() - 1); }
    return cachedLocalKey;
}

/**
 * If the composite key is either empty or has a single node, there is
 * no service name.
 * @return javax.naming.Name
 */
public synchronized Name getServiceName() 
{
    if ( size() < 2 )
        { return null; }
    if ( cachedServiceName == null )
        { cachedServiceName =  getCompositeKey().getPrefix(size() - 1); }
    return cachedServiceName;
}  
/**
 * Returns a new empty Name
 */
public Name newName() throws InvalidNameException
{
    return new NameImpl();
}
/**
 * @param newCompositeKey javax.naming.Name
 */
public synchronized void setCompositeKey(Name newCompositeKey)
{
    compositeKey = newCompositeKey;
    cachedCompositeKey = null;
    cachedLocalKey = null;
    cachedServiceName = null;

}
/**
 * @param newServiceName javax.naming.Name
 */
public void setServiceName(Name newServiceName) throws InvalidNameException
{
    Name newKey = newName().addAll(newServiceName).add(getLocalKey());
    setCompositeKey(newKey);
    cachedServiceName = newServiceName;
}
/**
 * @return int
 */
protected int size() 
{
    return getCompositeKey().size();
}
/**
 * Returns a String that represents the value of this object.
 * @return java.lang.String
 */
public String toString() {
    return "CompositeEntityIdentifier (" + type + "(" + getKey() + "))";
}
/**
 * Returns a CompoundName parsed from key
 */
public Name parseCompoundKey(String key) throws NamingException
{
    return NAME_PARSE_CACHE.getUnchecked(key);
}

private static final class NameImpl implements Name
{
    List<String> components;
    public NameImpl() {
        this(new ArrayList(4));
    }
    public NameImpl(List<String> comps) {
        super();
        components = comps;
    }
    public Name add(String comp) {
        components.add(comp);
        return this;
    }
    public Name add(int posn, String comp) {
        components.add(posn,comp);
        return this;
    }
    public Name addAll(int posn, Name n) {
        int i=posn;
        for ( Enumeration e=n.getAll(); e.hasMoreElements(); i++)
            { add( i, (String)e.nextElement() ); }
        return this;
    }
    public Name addAll(Name n) {
        for ( Enumeration e=n.getAll(); e.hasMoreElements(); )
            { add( (String)e.nextElement() ); }
        return this;
    }
    public Object clone() {
        List<String> comps = (List<String>)((ArrayList<String>)components).clone();
        return new NameImpl(comps);
    }
    public int compareTo(Object obj) 
    {
        if (this == obj) 
            { return 0; }
        if (!(obj instanceof Name)) 
            { throw new ClassCastException("Not a Name"); }

        Name name = (Name) obj;
        int len1 = size();
        int len2 = name.size();
        int n = Math.min(len1, len2);

        int index1 = 0, index2 = 0;

        while (n-- != 0) 
        {
            String comp1 = get(index1++);
            String comp2 = name.get(index2++);

            comp1 = comp1.trim();
            comp2 = comp2.trim();

            int local = comp1.compareTo(comp2);
            if (local != 0) 
                { return local; }
        }
        return len1 - len2;
    }
    public boolean endsWith(Name n) {
        int startIndex = size() - n.size();
        if (startIndex < 0 || startIndex > size()) 
            { return false; }
        Enumeration suffix = n.getAll();
        try
        {
            Enumeration mycomps = getAll();
            while (mycomps.hasMoreElements()) 
            {
                String my = (String)mycomps.nextElement();
                String his = (String)suffix.nextElement();
                my = my.trim();
                his = his.trim();
                if (!(my.equals(his)))
                    { return false; }
            }
        }
        catch (NoSuchElementException e) 
            { return false; }
        return true;
    }
    public String get(int posn) {
        return components.get(posn);
    }
    public Enumeration getAll() {
      return new NameImplEnumerator(components,0,components.size());
    }
    public Name getPrefix(int posn) {
        if ( posn < 0 || posn >= size() ) 
            { throw new ArrayIndexOutOfBoundsException(posn); }
        return getNameComponents(0,posn);
    }
    public Name getSuffix(int posn) {
        if (posn < 0 || posn > size()) 
            { throw new ArrayIndexOutOfBoundsException(posn); }
        return getNameComponents(posn,size());
    }
    public boolean isEmpty() {
        return (components.isEmpty());
    }
    public Object remove(int posn) throws InvalidNameException {
        if (posn < 0 || posn >= size() ) 
            { throw new InvalidNameException("Invalid position."); }
        return components.remove(posn);
    }
    public int size() {
        return (components.size());
    }
    public boolean startsWith(Name n) {
        Name myPrefix = getPrefix(n.size());
        return (myPrefix.compareTo(n) == 0);
    } 
    public String toString() {
        if (size() == 0)
            { return ""; }
        if (size() == 1)
            { return get(0); }

//  TODO: for jdk 1.5:
//        StringBuilder sb = new StringBuilder();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < size(); i++)
        {
            if (i != 0)
                { sb.append(separator); }
            sb.append(get(i));
        }
        return (sb.toString());
    }
    public boolean equals(Object obj) 
    {
        if ( obj == null )
            { return false; }
        if ( obj == this )
            { return true; }
        if ( ! (obj instanceof Name) )
            { return false; }
        Name target = (Name)obj;
        if ( target.size() != this.size() )
            { return false; }
        
        // For our purposes this is sufficient, if not entirely correct:
        return target.toString().equals(this.toString());
    } 
    public int hashCode() {
        int hash = 0;
        for (Enumeration e = getAll(); e.hasMoreElements();) {
            String comp = (String)e.nextElement();
            hash += comp.hashCode();
        }
        return hash;
    }
    private Enumeration<String> getComponents(int start, int limit) {
        return new NameImplEnumerator(components,start,limit);
    }
    private Name getNameComponents(int start, int limit) {
        List<String> comps = new ArrayList<String>(limit - start);
        for (Enumeration<String> e = getComponents(start,limit); e.hasMoreElements();)
            { comps.add(e.nextElement()); }
        return new NameImpl(comps);
    }
    
}
private static final class NameImplEnumerator implements Enumeration<String> {
    List<String> list;
    int count;
    int limit;

    NameImplEnumerator(List<String> l, int start, int lim) {
    list = l;
    count = start;
    limit = lim;
    }

    public boolean hasMoreElements() {
    return count < limit;
    }

    public String nextElement() {
    if (count < limit) {
        return list.get(count++);
    }
    throw new NoSuchElementException("NameImplEnumerator");
    }
}

}
