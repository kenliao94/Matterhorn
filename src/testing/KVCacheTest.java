package testing;

import cache.KVFIFOCache;
import cache.KVLFUCache;
import cache.KVLRUCache;
import junit.framework.TestCase;

import org.junit.Test;
import org.junit.Assert;


/**
 * Tests the KVFIFOCache, KVLRUCache, and KVLFUCache classes, by setting, getting, and deleting values from the caches.
 */
public class KVCacheTest extends TestCase {
	
	@Test
	public void testFIFOGetSet() {
		KVFIFOCache fifoCache = new KVFIFOCache(2);
		fifoCache.set("key1", "val1");
		fifoCache.set("key2", "val2");
		fifoCache.set("key1", "val1");
		fifoCache.set("key3", "val3");
		Assert.assertEquals(null, fifoCache.get("key1"));
		Assert.assertEquals("val2", fifoCache.get("key2"));
	}
	
	@Test
	public void testFIFODelete() {
		KVFIFOCache fifoCache = new KVFIFOCache(3);
		fifoCache.set("key1", "val1");
		fifoCache.set("key2", "val2");
		fifoCache.set("key3", "val3");
		fifoCache.delete("key2");
		Assert.assertEquals("val1", fifoCache.get("key1"));
		Assert.assertEquals(null, fifoCache.get("key2"));
		Assert.assertEquals("val3", fifoCache.get("key3"));
	}
	
	@Test
	public void testLRUGetSet() {
		KVLRUCache lruCache = new KVLRUCache(2);
		lruCache.set("key1", "val1");
		lruCache.set("key2", "val2");
		lruCache.set("key1", "val1");
		lruCache.set("key3", "val3");
		Assert.assertEquals("val1", lruCache.get("key1"));
		Assert.assertEquals(null, lruCache.get("key2"));
	}
	
	@Test
	public void testLRUDelete() {
		KVLRUCache lruCache = new KVLRUCache(2);
		lruCache.set("key1", "val1");
		lruCache.set("key2", "val2");
		lruCache.set("key1", "val1");
		lruCache.set("key3", "val3");
		lruCache.delete("key3");
		Assert.assertEquals("val1", lruCache.get("key1"));
		Assert.assertEquals(null, lruCache.get("key2"));
		Assert.assertEquals(null, lruCache.get("key3"));
	}
	
	@Test
	public void testLFUGetSet() {
		KVLFUCache lfuCache = new KVLFUCache(2);
		lfuCache.set("key1", "val1");
		lfuCache.get("key1");
		lfuCache.set("key1", "val1");
		lfuCache.set("key2", "val2");
		lfuCache.get("key2");
		lfuCache.set("key3", "val3");
		Assert.assertEquals("val1", lfuCache.get("key1"));
		Assert.assertEquals(null, lfuCache.get("key2"));
	}
	
	@Test
	public void testLFUDelete() {
		KVLFUCache lfuCache = new KVLFUCache(2);
		lfuCache.set("key1", "val1");
		lfuCache.get("key1");
		lfuCache.set("key1", "val1");
		lfuCache.set("key2", "val2");
		lfuCache.get("key2");
		lfuCache.set("key3", "val3");
		lfuCache.delete("key2");
		lfuCache.delete("key1");
		Assert.assertEquals(null, lfuCache.get("key1"));
		Assert.assertEquals(null, lfuCache.get("key2"));
		Assert.assertEquals("val3", lfuCache.get("key3"));
	}
}
