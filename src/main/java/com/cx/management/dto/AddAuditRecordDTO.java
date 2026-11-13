package com.cx.management.dto;

import com.cx.management.util.AbstractObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author suncaixiang
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddAuditRecordDTO extends AbstractObject {
    private static final long serialVersionUID = 1L;

    private String platform;

    private String accountNo;

    private String tradeStartDate;

    private String tradeEndDate;

    private MultipartFile multipartFile;

}
