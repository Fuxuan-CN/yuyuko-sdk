package me.yuyuko.sdk.io.memory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 内存缓存操作
 * @author castorice (遐蝶)
 * @param <KT> 键的类型
 * @param <VT> 值的类型
*/
public class MemoryCache<KT, VT> {
    private final ConcurrentMap<KT, VT> memCache = new ConcurrentHashMap<KT, VT>();

    public void put(KT key, VT value) {
        memCache.put(key, value);
    }

    public VT get(KT key) {
        return memCache.get(key);
    }

    public boolean containsKey(KT key) {
        return memCache.containsKey(key);
    }

    public void remove(KT key) {
        memCache.remove(key);
    }

    public void clear() {
        memCache.clear();
    }

    public int size() {
        return memCache.size();
    }

    public boolean isEmpty()
    {
        return memCache.isEmpty();
    }
}
