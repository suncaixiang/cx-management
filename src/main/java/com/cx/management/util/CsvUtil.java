package com.cx.management.util;

import com.cx.management.common.exception.BusinessException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.StringReader;
import java.util.List;

/**
 * 处理csv
 *
 * @author : scx
 * @date : 2025-07-21
 * Description:
 */
@Slf4j
public class CsvUtil {

    /**
     * csv文本信息转换成bean实体
     *
     * @param csvContent csv文本
     * @param clazz      转换成的bean
     * @return 空字符串，返回空列表
     */
    public static <T> List<T> convertCsvToBean(String csvContent, Class<T> clazz) {
        if (!StringUtils.hasText(csvContent)) {
            return Lists.newArrayList();
        }

        try {
            CsvMapper mapper = new CsvMapper();
            // 使用第一行作为header
            CsvSchema schema = CsvSchema.emptySchema().withHeader();
            MappingIterator<T> it = mapper.readerFor(clazz)
                    .with(schema)
                    .readValues(new StringReader(csvContent));
            return it.readAll();
        } catch (Exception e) {
            log.error("csv文本信息转换成bean实体异常，csvContent:{},异常信息:{}",csvContent, e.getMessage(), e);
            throw new BusinessException("csv文本信息转换成bean实体异常," + e.getMessage());
        }
    }

}
