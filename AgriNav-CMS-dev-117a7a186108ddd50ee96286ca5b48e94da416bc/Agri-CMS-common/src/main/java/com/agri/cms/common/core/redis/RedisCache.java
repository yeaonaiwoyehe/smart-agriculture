package com.agri.cms.common.core.redis;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.agri.cms.common.utils.DateUtils;
import com.alibaba.fastjson2.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.*;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.domain.geo.GeoLocation;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.stereotype.Component;

/**
 * spring redis 工具类
 *
 * @author ruoyi
 **/
@SuppressWarnings(value = {"unchecked", "rawtypes"})
@Component
public class RedisCache {
    @Autowired
    public RedisTemplate redisTemplate;

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key   缓存的键值
     * @param value 缓存的值
     */
    public <T> void setCacheObject(final String key, final T value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key      缓存的键值
     * @param value    缓存的值
     * @param timeout  时间
     * @param timeUnit 时间颗粒度
     */
    public <T> void setCacheObject(final String key, final T value, final Integer timeout, final TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }
    public <T> void setCacheObject(final String key, final T value, final Long timeout, final TimeUnit timeUnit)
    {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
     * 设置有效时间
     *
     * @param key     Redis键
     * @param timeout 超时时间
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout) {
        return expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 设置有效时间
     *
     * @param key     Redis键
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout, final TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 获取有效时间
     *
     * @param key Redis键
     * @return 有效时间
     */
    public long getExpire(final String key) {
        return redisTemplate.getExpire(key);
    }

    /**
     * 判断 key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 获得缓存的基本对象。
     *
     * @param key 缓存键值
     * @return 缓存键值对应的数据
     */
    public <T> T getCacheObject(final String key) {
        ValueOperations<String, T> operation = redisTemplate.opsForValue();
        return operation.get(key);
    }

    public void incrementCacheObject(final String key,final String member){
        redisTemplate.opsForHash().increment(key, member, 1);
    }



    /**
     * 删除单个对象
     *
     * @param key
     */
    public boolean deleteObject(final String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 删除集合对象
     *
     * @param collection 多个对象
     * @return
     */
    public boolean deleteObject(final Collection collection) {
        return redisTemplate.delete(collection) > 0;
    }

    /**
     * 缓存List数据
     *
     * @param key      缓存的键值
     * @param dataList 待缓存的List数据
     * @return 缓存的对象
     */
    public <T> long setCacheList(final String key, final List<T> dataList) {
        Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
        return count == null ? 0 : count;
    }

    /**
     * 获得缓存的list对象
     *
     * @param key 缓存的键值
     * @return 缓存键值对应的数据
     */
    public <T> List<T> getCacheList(final String key) {
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    /**
     * 缓存Set
     *
     * @param key     缓存键值
     * @param dataSet 缓存的数据
     * @return 缓存数据的对象
     */
    public <T> BoundSetOperations<String, T> setCacheSet(final String key, final Set<T> dataSet) {
        BoundSetOperations<String, T> setOperation = redisTemplate.boundSetOps(key);
        if (dataSet != null) {
            dataSet.forEach(t -> {
                // 使用 parseObject 而不是 parse，保持类型信息
                String jsonString = JSON.toJSONString(t);
                Object cleanObject = JSON.parseObject(jsonString, t.getClass());
                setOperation.add((T) cleanObject);
            });
        }
        return setOperation;
    }
    //删除缓存Set中的指定元素
    public <T> boolean deleteCacheSetValue(final String key, final String value) {
        return redisTemplate.opsForSet().remove(key, value) > 0;
    }

    /**
     * 获得缓存的set
     *
     * @param key
     * @return
     */
    public <T> Set<T> getCacheSet(final String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 获取多个key的并集
     * @param key
     * @param otherKey
     * @return Set<T>
     * @author simon
     * @since 2024/11/22
     */
    public <T> Set<T> getCacheSetUnion (final String key,final String ...otherKey) {
        return redisTemplate.opsForSet().union(key, otherKey);
    }

    /**
     * 获取多个key的差集
     * @param key
     * @param otherKey
     * @return Set<T>
     * @author simon
     * @since 2024/11/22
     */
    public <T> Set<T> getCacheSetDiff(final String key, final String ...otherKey) {
        return redisTemplate.opsForSet().difference(key, Arrays.asList(otherKey));
    }

    /**
     * 缓存Map
     *
     * @param key
     * @param dataMap
     */
    public <T> void setCacheMap(final String key, final Map<String, T> dataMap) {
        if (dataMap != null) {
            redisTemplate.opsForHash().putAll(key, dataMap);
        }
    }

    /**
     * 获得缓存的Map
     *
     * @param key
     * @return
     */
    public <T> Map<String, T> getCacheMap(final String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 往Hash中存入数据
     *
     * @param key   Redis键
     * @param hKey  Hash键
     * @param value 值
     */
    public <T> void setCacheMapValue(final String key, final String hKey, final T value) {
        redisTemplate.opsForHash().put(key, hKey, value);
    }

    /**
     * 获取Hash中的数据
     *
     * @param key  Redis键
     * @param hKey Hash键
     * @return Hash中的对象
     */
    public <T> T getCacheMapValue(final String key, final String hKey) {
        HashOperations<String, String, T> opsForHash = redisTemplate.opsForHash();
        return opsForHash.get(key, hKey);
    }

    public <T> T getCacheMapLastValue(String key) {
        Set<String> keys = redisTemplate.opsForHash().keys(key);
        if (keys.isEmpty()) {
            return null;
        }
        // 排序获取最后一个键
        String lastKey = keys.stream()
                .map(Object::toString)
                .sorted()
                .toList()
                .get(keys.size() - 1);
        HashOperations<String, String, T> opsForHash = redisTemplate.opsForHash();
        return opsForHash.get(key, lastKey);
    }
    /**
     * 获取多个Hash中的数据
     *
     * @param key   Redis键
     * @param hKeys Hash键集合
     * @return Hash对象集合
     */
    public <T> List<T> getMultiCacheMapValue(final String key, final Collection<Object> hKeys) {
        return redisTemplate.opsForHash().multiGet(key, hKeys);
    }

    /**
     * 删除Hash中的某条数据
     *
     * @param key  Redis键
     * @param hKey Hash键
     * @return 是否成功
     */
    public boolean deleteCacheMapValue(final String key, final String hKey) {
        return redisTemplate.opsForHash().delete(key, hKey) > 0;
    }

    /**
     * 获得缓存的基本对象列表
     *
     * @param pattern 字符串前缀
     * @return 对象列表
     */
    public Collection<String> keys(final String pattern) {
        return redisTemplate.keys(pattern);
    }

    public void setCacheGeoValue(final String key, final Point point, final Long member) {
        GeoOperations<String, Long> geoOperations = redisTemplate.opsForGeo();
        geoOperations.add(key, point, member);
    }

    public void deleteCacheGeoValue(final String key, final Long member) {
        GeoOperations<String, Long> geoOperations = redisTemplate.opsForGeo();
        geoOperations.remove(key, member);
    }

    /**
     * 根据经纬度查询符合条件的地块
     *
     * @param key       Redis键
     * @param longitude 经度
     * @param latitude  纬度
     * @param distance  距离范围
     * @return List<T>
     * @author simon
     * @since 2024/9/23
     */
    public <T> List<T> getCacheGeoValue(final String key, final Double longitude, final Double latitude, final Double distance) {
        //通过上传的经纬度构造位置点和搜索环
        Circle circle = new Circle(new Point(longitude, latitude), new Distance(distance, Metrics.METERS));
        //查询出符合条件的位置结果
        GeoResults<GeoLocation<T>> radius = redisTemplate.opsForGeo().radius(key, circle);
        //通过流式操作筛选出结果地块的ID集合并返回
        return Objects.isNull(radius) ? List.of() : radius.getContent().stream()
                .map(GeoResult::getContent)
                .map(GeoLocation::getName)
                .toList();
    }

    /**
     * 依据键前缀扫描出所有键
     *
     * @param pattern
     * @return Set<String>
     * @author 李鸿麟
     * @since 2024/10/30
     */
    public Set<String> scanKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            try (Cursor<byte[]> cursor = connection.scan(
                    ScanOptions.scanOptions()
                            .match(pattern)
                            .count(1000)
                            .build())) {

                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
                return keys;
            }
        });
        return keys;
    }

    /**
     * 将元素添加到 HyperLogLog
     *
     * @param key   Redis键
     * @param value 要添加的值
     * @return 添加成功返回true，失败返回false
     * @author 李鸿麟
     */

    public void pfadd(String key, String value) {
        try {
            redisTemplate.opsForHyperLogLog().add(key, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取 HyperLogLog 的基数估计值
     *
     * @param key Redis键
     * @return 基数估计值
     * @author 李鸿麟
     */

    public Long pfcount(String key) {
        try {
            return redisTemplate.opsForHyperLogLog().size(key + DateUtils.getDate());
        } catch (Exception e) {
            // log.error("Redis pfcount error: ", e);
            return 0L;
        }
    }

}
