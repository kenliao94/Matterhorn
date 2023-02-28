package cache;
import java.util.LinkedHashMap;
import java.util.Map;

public class KVLRUCache implements KVCache {
    private final int capacity;
    private LinkedHashMap<String, String> map;
    
    public KVLRUCache(int cap) {
        capacity = cap;
        map = new LinkedHashMap<String, String>(capacity, 0.75f, true){
            /**
			 * Supply default serial ID.
			 */
			private static final long serialVersionUID = 1L;

			protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > capacity;
            }
        };
    }
    
    public synchronized String get(String key) {
        return map.getOrDefault(key, null);
    }
    
    public synchronized int getSize() {
        return map.size();
    }
    
    public synchronized void set(String key, String value) {
        map.put(key, value);
    }
    
    public synchronized void delete(String key) {
        map.remove(key);
    }
    
}