package com.cx.management.util;

import com.google.common.collect.Lists;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author suncaixiang
 */
public class ObjectUtils {

    public ObjectUtils() {
    }

    public static <T> List<T> convertList(List<? extends AbstractObject> sourceList, Class<T> targetClazz) {
        if (CollectionUtils.isEmpty(sourceList)) {
            return Lists.newArrayList();
        } else {
            List<T> targetList = new ArrayList();
            Iterator var3 = sourceList.iterator();

            while(var3.hasNext()) {
                AbstractObject sourceObject = (AbstractObject)var3.next();
                if (sourceObject != null) {
                    targetList.add(sourceObject.clone(targetClazz));
                }
            }

            return targetList;
        }
    }
}
