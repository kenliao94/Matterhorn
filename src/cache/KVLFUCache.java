package cache;

import java.util.HashMap;
import java.util.LinkedHashSet;

public class KVLFUCache implements KVCache {
    HashMap<String, String> mainMap;
    HashMap<String, Integer> countMap;
    HashMap<Integer, LinkedHashSet<String>> countListMap;
    int capacity;
    int minCount = -1;
    public KVLFUCache(int cap) {
        capacity = cap;
        mainMap = new HashMap<>();
        countMap = new HashMap<>();
        countListMap = new HashMap<>();
        countListMap.put(1, new LinkedHashSet<String>());
    }
    
    public synchronized String get(String key) {
        if(!mainMap.containsKey(key))
            return null;
        int count = countMap.get(key);
        countMap.put(key, count+1);
        countListMap.get(count).remove(key);
        if(count == minCount && countListMap.get(count).size()==0)
            minCount++;
        if(!countListMap.containsKey(count+1))
            countListMap.put(count+1, new LinkedHashSet<String>());
        countListMap.get(count+1).add(key);
        return mainMap.get(key);
    }
    
    public synchronized void set(String key, String value) {
        if(capacity<=0)
            return;
        if(mainMap.containsKey(key)) {
            mainMap.put(key, value);
            get(key);
            return;
        } 
        if(mainMap.size() >= capacity) {
            String evict = countListMap.get(minCount).iterator().next();
            countListMap.get(minCount).remove(evict);
            mainMap.remove(evict);
        }
        mainMap.put(key, value);
        countMap.put(key, 1);
        minCount = 1;
        countListMap.get(1).add(key);
    }
    
    public synchronized void delete(String key) {
    	if(!mainMap.containsKey(key))
            return;
    	int count = countMap.get(key);
    	countListMap.get(count).remove(key);
    	countMap.remove(key);
    	mainMap.remove(key);
    	
    	
    }
    
}