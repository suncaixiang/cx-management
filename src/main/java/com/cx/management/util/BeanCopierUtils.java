package com.cx.management.util;

import org.springframework.cglib.beans.BeanCopier;
import org.springframework.cglib.core.Converter;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 *
 * @author suncaixiang
 */
public class BeanCopierUtils {

    private static Map<String, BeanCopier> beanCopierCacheMap = new HashMap();

    public BeanCopierUtils() {
    }

    public static void copyProperties(Object source, Object target) {
        String cacheKey = source.getClass().toString() + target.getClass().toString();
        BeanCopier beanCopier;
        if (!beanCopierCacheMap.containsKey(cacheKey)) {
            Class var4 = BeanCopierUtils.class;
            synchronized(BeanCopierUtils.class) {
                if (!beanCopierCacheMap.containsKey(cacheKey)) {
                    beanCopier = BeanCopier.create(source.getClass(), target.getClass(), false);
                    beanCopierCacheMap.put(cacheKey, beanCopier);
                } else {
                    beanCopier = (BeanCopier)beanCopierCacheMap.get(cacheKey);
                }
            }
        } else {
            beanCopier = (BeanCopier)beanCopierCacheMap.get(cacheKey);
        }

        beanCopier.copy(source, target, (Converter)null);
    }
}
