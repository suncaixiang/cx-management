package com.cx.management.util;

import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从交易账户备注中解析月日信息，用于展示创建/重置/回收时间。
 */
public final class TradingCommentDateParser {

    private static final DateTimeFormatter OUTPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final Pattern DOT_DATE_PATTERN = Pattern.compile(
            "^(\\d{1,2})[./,、。](\\d{1,2})");
    private static final Pattern MONTH_DAY_PATTERN = Pattern.compile(
            "^(\\d{1,2})月(\\d{1,2})日?");

    private TradingCommentDateParser() {
    }

    public static String parseToDateString(String comment) {
        LocalDate date = parse(comment);
        return date == null ? null : date.format(OUTPUT_FORMATTER);
    }

    public static LocalDate parse(String comment) {
        if (!StringUtils.hasText(comment)) {
            return null;
        }
        String trimmed = comment.trim();
        Matcher dotMatcher = DOT_DATE_PATTERN.matcher(trimmed);
        if (dotMatcher.find()) {
            return buildDate(dotMatcher.group(1), dotMatcher.group(2));
        }
        Matcher monthDayMatcher = MONTH_DAY_PATTERN.matcher(trimmed);
        if (monthDayMatcher.find()) {
            return buildDate(monthDayMatcher.group(1), monthDayMatcher.group(2));
        }
        return null;
    }

    private static LocalDate buildDate(String monthText, String dayText) {
        try {
            int month = Integer.parseInt(monthText);
            int day = Integer.parseInt(dayText);
            if (month < 1 || month > 12 || day < 1 || day > 31) {
                return null;
            }
            LocalDate today = LocalDate.now();
            int year = today.getYear();
            LocalDate candidate = LocalDate.of(year, month, day);
            if (candidate.isAfter(today)) {
                candidate = candidate.minusYears(1);
            }
            return candidate;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
