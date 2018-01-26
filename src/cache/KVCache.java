package cache;

public interface KVCache {
	/**
     * Get the value associated with the given key
     * @return  string value
     */
	public String get(String key);
	
	/**
     * Set the given cache key to the given value
     */
	public void set(String key, String value);
}
