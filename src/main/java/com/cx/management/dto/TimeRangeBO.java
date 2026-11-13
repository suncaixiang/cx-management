package com.cx.management.dto;

/**
 * @author suncaixiang
 */

import lombok.*;

import java.time.LocalTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TimeRangeBO {

    /**
     * 开始时间
     */
    private LocalTime startLocalTime;

    /**
     * 结束时间
     */
    private LocalTime endLocalTime;

    /**
     * 是否间隔一天
     */
    private boolean crossesMidnight;

    public static TimeRangeBO of(String startTime, String endTime) {
        return new TimeRangeBO(startTime, endTime);
    }

    public TimeRangeBO(String startTime, String endTime) {
        this.startLocalTime = LocalTime.parse(startTime).minusMinutes(1);
        this.endLocalTime = LocalTime.parse(endTime);
        this.crossesMidnight = startLocalTime.isAfter(endLocalTime);
    }

    public boolean contains(LocalTime time) {
        //跨天的，
        if (crossesMidnight) {
            //当前时间大于开始时间或者当前时间小于结束时间
            return !time.isBefore(startLocalTime) || !time.isAfter(endLocalTime);
        }
        //当前时间在开始-结束之间
        return !time.isBefore(startLocalTime) && !time.isAfter(endLocalTime);
    }
}
