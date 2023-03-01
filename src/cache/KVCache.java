package cache;

public interface KVCache {
	/**
     * Get the value associated with the given key
     * @return  string value
     */
	public String get(String key);
	
	/**
     * Set the given cache key to the given value
     * @param key The key to be set
     * @param value The value to be set
     */
	public void set(String key, String value);
	
	public void delete(String key);
}
