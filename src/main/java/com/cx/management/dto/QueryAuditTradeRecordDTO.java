package com.cx.management.dto;

import lombok.*;

/**
 * @author suncaixiang
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class QueryAuditTradeRecordDTO {
    private static final long serialVersionUID = 1L;

    private Integer current;

    private Integer size;

    private Long auditRecordId;

    private Integer status;

}
