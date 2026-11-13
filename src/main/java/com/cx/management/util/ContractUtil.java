package com.cx.management.util;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author suncaixiang
 */
public class ContractUtil {

    private static final Pattern PATTERN =
            Pattern.compile("([a-zA-Z]+)(\\d+)");

    /**
     * 统一成 yyMM 格式
     * PF604 -> PF2604
     * PF2604 -> PF2604
     */
    public static String normalize(String contract) {

        contract = contract.toUpperCase();

        Matcher m = PATTERN.matcher(contract);

        if (!m.matches()) {
            return contract;
        }

        String symbol = m.group(1);
        String num = m.group(2);

        // 已经是4位
        if (num.length() == 4) {
            return contract;
        }

        // 3位
        if (num.length() == 3) {

            int shortYear = Integer.parseInt(num.substring(0,1));
            String month = num.substring(1);

            int fullYear = resolveYear(shortYear);

            return symbol + (fullYear % 100) + month;
        }

        return contract;
    }



    /**
     * 根据当前年份推断完整年份
     */
    private static int resolveYear(int shortYear) {

        int currentYear = LocalDate.now().getYear();

        int base = (currentYear / 10) * 10;

        int year = base + shortYear;

        if (year < currentYear - 5) {
            year += 10;
        }

        if (year > currentYear + 5) {
            year -= 10;
        }

        return year;
    }

    public static void main(String[] args) {

        System.out.println(normalize("OI609"));
        System.out.println(normalize("PF2604"));

    }
}