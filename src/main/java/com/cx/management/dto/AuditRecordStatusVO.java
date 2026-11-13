package com.cx.management.dto;

import com.cx.management.util.AbstractObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author suncaixiang
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditRecordStatusVO extends AbstractObject {

    private Long id;


    private Integer totalNum;

    private Long doneNum;
}
