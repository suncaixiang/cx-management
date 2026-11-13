package com.cx.management.util;

import com.cx.management.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author suncaixiang
 */
public class AbstractObject {

    private static final Logger log = LoggerFactory.getLogger(AbstractObject.class);

    public AbstractObject() {
    }

    public <T> T clone(Class<T> targetClazz) {
        try {
            T target = targetClazz.newInstance();
            BeanCopierUtils.copyProperties(this, target);
            return target;
        } catch (Exception var3) {
            Exception e = var3;
            log.error("转换数据异常，className:{}", targetClazz.getName());
            throw new BusinessException(e.getMessage());
        }
    }

    public <T> T clone(T target) {
        BeanCopierUtils.copyProperties(this, target);
        return target;
    }
}
