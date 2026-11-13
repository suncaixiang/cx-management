package com.cx.management.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cx.management.common.PageResult;
import com.cx.management.common.exception.BusinessException;
import com.cx.management.dto.*;
import com.cx.management.entity.AuditDisableProduct;
import com.cx.management.entity.AuditRecord;
import com.cx.management.entity.AuditTradeRecord;
import com.cx.management.enums.AuditPlatform;
import com.cx.management.enums.AuditRecordStatusEnum;
import com.cx.management.enums.AuditTradeRecordStatusEnum;
import com.cx.management.excel.ByFileParse;
import com.cx.management.excel.WhNewFileParse;
import com.cx.management.mapper.AuditDisableProductMapper;
import com.cx.management.mapper.AuditRecordMapper;
import com.cx.management.mapper.AuditTradeRecordMapper;
import com.cx.management.util.RqCache;
import com.cx.management.util.RqInstrumentBO;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditRecordFacade {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AuditRecordMapper auditRecordMapper;
    private final AuditTradeRecordMapper auditTradeRecordMapper;
    private final AuditRecordService auditRecordService;
    private final AuditPublisher auditPublisher;
    private final AuditDisableProductMapper auditDisableProductMapper;
    private final AuditTradeRecordService auditTradeRecordService;
    private final BatchExecuteFacade batchExecuteFacade;

    public List<AuditPlatformVO> listPlatforms() {
        return Arrays.stream(AuditPlatform.values())
                .map(item -> new AuditPlatformVO(item.getCode(), item.getLabel()))
                .collect(Collectors.toList());
    }

    public PageResult<AuditRecordVO> pageRecords(int current, int size, String accountNo, String platform) {
        Page<AuditRecord> page = new Page<>(current, size);
        LambdaQueryWrapper<AuditRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(accountNo)) {
            wrapper.like(AuditRecord::getAccountNo, accountNo.trim());
        }
        if (StringUtils.hasText(platform)) {
            wrapper.eq(AuditRecord::getPlatform, platform.trim());
        }
        wrapper.orderByDesc(AuditRecord::getId);
        Page<AuditRecord> result = auditRecordMapper.selectPage(page, wrapper);
        List<AuditRecordVO> records = result.getRecords().stream()
                .map(this::toRecordVO)
                .collect(Collectors.toList());
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public AuditRecordVO getRecord(Long id) {
        return toRecordVO(requireRecord(id));
    }

    public AuditRecordStatusVO queryStatus(Long id) {
        AuditRecord auditRecordDO = auditRecordService.getById(id);
        if (auditRecordDO == null) {
            return null;
        }
        Long doneNum = auditTradeRecordService.countByStatus(id, Lists.newArrayList(AuditTradeRecordStatusEnum.PASS.getCode(), AuditTradeRecordStatusEnum.FAIL.getCode()));
        return AuditRecordStatusVO.builder().id(id).totalNum(auditRecordDO.getTradeNum()).doneNum(doneNum).build();
    }


    @Transactional(rollbackFor = Exception.class)
    public Long createRecord(AddAuditRecordDTO dto) {
        log.info("开始创建审核记录, accountNo:{}, platform:{}, start:{}, end:{}",
                dto.getAccountNo(), dto.getPlatform(), dto.getTradeStartDate(), dto.getTradeEndDate());
        if (!StringUtils.hasText(dto.getAccountNo())) {
            throw new BusinessException("账号不能为空");
        }
        if (!AuditPlatform.isValid(dto.getPlatform())) {
            throw new BusinessException("平台无效");
        }
        String start = normalizeDate(dto.getTradeStartDate(), "交易开始日期");
        String end = normalizeDate(dto.getTradeEndDate(), "交易结束日期");
        if (end.compareTo(start) < 0) {
            throw new BusinessException("交易结束日期不能早于开始日期");
        }
        if (dto.getMultipartFile() == null || dto.getMultipartFile().isEmpty()) {
            throw new BusinessException("请上传xlsx文件");
        }
        String originalName = dto.getMultipartFile().getOriginalFilename() == null ? "" : dto.getMultipartFile().getOriginalFilename().trim();
        if (!originalName.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new BusinessException("上传文件格式必须为xlsx");
        }
        if (originalName.length() > 100) {
            throw new BusinessException("上传文件名称过长");
        }

        //解析上传文件，判断是否符合格式要求.
        List<TradeRecordDTO> tradeRecordDTOList = readExcel(dto);
        log.info("审核文件解析完成, accountNo:{}, tradeNum:{}", dto.getAccountNo(), tradeRecordDTOList.size());

        //保存审核记录数据
        AuditRecord auditRecordDO = adaptor(dto, tradeRecordDTOList.size());
        auditRecordMapper.insert(auditRecordDO);
        log.info("审核记录已入库, id:{}, accountNo:{}, tradeNum:{}",
                auditRecordDO.getId(), auditRecordDO.getAccountNo(), auditRecordDO.getTradeNum());

        //保存成交明细数据（大文件时这里最耗时）
        long enrichStart = System.currentTimeMillis();
        List<AuditTradeRecord> auditTradeRecordDOList = auditTradeRecordDOList(tradeRecordDTOList, dto, auditRecordDO);
        log.info("成交明细组装完成, id:{}, size:{}, costMs:{}",
                auditRecordDO.getId(), auditTradeRecordDOList.size(), System.currentTimeMillis() - enrichStart);

        long saveStart = System.currentTimeMillis();
        boolean result = auditTradeRecordService.saveBatch(auditTradeRecordDOList, 1000);
        log.info("成交明细入库完成, id:{}, size:{}, success:{}, costMs:{}",
                auditRecordDO.getId(), auditTradeRecordDOList.size(), result, System.currentTimeMillis() - saveStart);
        if (result) {
            auditPublisher.publishStartAudit(auditRecordDO);
        }
        return auditRecordDO.getId();
    }

    private List<AuditTradeRecord> auditTradeRecordDOList(List<TradeRecordDTO> tradeRecordDTOList,
                                                          AddAuditRecordDTO dto,
                                                          AuditRecord auditRecordDO) {
        Set<String> instrumentIdSet = tradeRecordDTOList.stream().map(TradeRecordDTO::getInstrumentId).collect(Collectors.toSet());
        //文件数据解析，存入成交记录
        Map<String, RqInstrumentBO> rqInstrumentMap = queryRqInstrument(instrumentIdSet);
        return tradeRecordDTOList.stream().map(tradeRecordDTO -> {
            AuditTradeRecord auditTradeRecordDO = tradeRecordDTO.clone(AuditTradeRecord.class);
            auditTradeRecordDO.setCloseProfit(new BigDecimal(tradeRecordDTO.getCloseProfit()));
            auditTradeRecordDO.setTradePrice(new BigDecimal(tradeRecordDTO.getTradePrice()));
            auditTradeRecordDO.setTradeVolume(Integer.valueOf(tradeRecordDTO.getTradeVolume()));
            //获取合约对应的productId/exchange
            auditTradeRecordDO.setProductId(rqInstrumentMap.get(tradeRecordDTO.getInstrumentId()).getUnderlyingSymbol());
            auditTradeRecordDO.setExchange(rqInstrumentMap.get(tradeRecordDTO.getInstrumentId()).getExchange());
            auditTradeRecordDO.setAuditRecordId(auditRecordDO.getId());
            auditTradeRecordDO.setAccountNo(dto.getAccountNo());
            auditTradeRecordDO.setPlatform(dto.getPlatform());
            auditTradeRecordDO.setStatus(AuditTradeRecordStatusEnum.INIT.getCode());
            return auditTradeRecordDO;
        }).collect(Collectors.toList());
    }

    private Map<String, RqInstrumentBO> queryRqInstrument(Collection<String> instrumentIdList) {
        List<RqInstrumentBO> instrumentBOList = RqCache.queryInstrumentList(instrumentIdList);
        if (CollectionUtils.isEmpty(instrumentBOList) || instrumentBOList.size() != instrumentIdList.size()) {
            throw new BusinessException("合约未查询到数据");
        }
        return instrumentBOList.stream().collect(Collectors.toMap(RqInstrumentBO::getOrderBookId, x -> x));
    }


    private static AuditRecord adaptor(AddAuditRecordDTO dto, Integer totalTradeNum) {
        AuditRecord auditRecordDO = new AuditRecord();
        auditRecordDO.setAccountNo(dto.getAccountNo());
        auditRecordDO.setPlatform(dto.getPlatform());
        auditRecordDO.setTradeStartDate(dto.getTradeStartDate());
        auditRecordDO.setTradeEndDate(dto.getTradeEndDate());
        auditRecordDO.setTradeNum(totalTradeNum);
        auditRecordDO.setStatus(AuditRecordStatusEnum.INIT.getCode());
        return auditRecordDO;
    }

    private static List<TradeRecordDTO> readExcel(AddAuditRecordDTO dto) {
        if (AuditPlatform.B_Y.getCode().equals(dto.getPlatform())) {
            return new ByFileParse().readExcel(dto);
        }
        return new WhNewFileParse().readExcel(dto);
    }


    public PageResult<AuditDisableProductVO> pageDisableProducts(int current, int size,
                                                                 String platform, String productId) {
        Page<AuditDisableProduct> page = new Page<>(current, size);
        LambdaQueryWrapper<AuditDisableProduct> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(platform)) {
            wrapper.eq(AuditDisableProduct::getPlatform, platform.trim());
        }
        if (StringUtils.hasText(productId)) {
            wrapper.like(AuditDisableProduct::getProductId, productId.trim());
        }
        wrapper.orderByDesc(AuditDisableProduct::getId);
        Page<AuditDisableProduct> result = auditDisableProductMapper.selectPage(page, wrapper);
        List<AuditDisableProductVO> records = result.getRecords().stream()
                .map(this::toDisableVO)
                .collect(Collectors.toList());
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional(rollbackFor = Exception.class)
    public AuditDisableProductVO createDisableProduct(AuditDisableProductRequest request) {
        if (request == null) {
            throw new BusinessException("参数不能为空");
        }
        String platform = request.getPlatform() == null ? "" : request.getPlatform().trim().toUpperCase(Locale.ROOT);
        String productId = request.getProductId() == null ? "" : request.getProductId().trim();
        String productName = request.getProductName() == null ? "" : request.getProductName().trim();
        if (!AuditPlatform.isValid(platform)) {
            throw new BusinessException("平台无效");
        }
        if (!StringUtils.hasText(productId)) {
            throw new BusinessException("品种代码不能为空");
        }
        if (!StringUtils.hasText(productName)) {
            throw new BusinessException("品种名称不能为空");
        }
        Long exists = auditDisableProductMapper.selectCount(new LambdaQueryWrapper<AuditDisableProduct>()
                .eq(AuditDisableProduct::getPlatform, platform)
                .eq(AuditDisableProduct::getProductId, productId));
        if (exists != null && exists > 0) {
            throw new BusinessException("该平台下品种代码已存在");
        }
        AuditDisableProduct entity = new AuditDisableProduct();
        entity.setPlatform(platform);
        entity.setProductId(productId);
        entity.setProductName(productName);
        auditDisableProductMapper.insert(entity);
        return toDisableVO(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDisableProduct(Long id) {
        if (id == null) {
            throw new BusinessException("禁用产品不存在");
        }
        AuditDisableProduct entity = auditDisableProductMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("禁用产品不存在");
        }
        auditDisableProductMapper.deleteById(id);
    }

    private String normalizeDate(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(fieldName + "不能为空");
        }
        String trimmed = value.trim();
        try {
            return LocalDate.parse(trimmed, DATE_FMT).format(DATE_FMT);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(fieldName + "格式必须为yyyyMMdd");
        }
    }

    private AuditRecord requireRecord(Long id) {
        if (id == null) {
            throw new BusinessException("审核记录不存在");
        }
        AuditRecord record = auditRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("审核记录不存在");
        }
        return record;
    }

    private AuditRecordVO toRecordVO(AuditRecord record) {
        AuditRecordVO vo = new AuditRecordVO();
        vo.setId(record.getId());
        vo.setAccountNo(record.getAccountNo());
        vo.setPlatform(record.getPlatform());
        AuditPlatform platform = AuditPlatform.fromCode(record.getPlatform());
        vo.setPlatformLabel(platform == null ? record.getPlatform() : platform.getLabel());
        vo.setTradeStartDate(record.getTradeStartDate());
        vo.setTradeEndDate(record.getTradeEndDate());
        vo.setTradeNum(record.getTradeNum());
        vo.setStatus(record.getStatus());
        vo.setStatusLabel(AuditRecordStatusEnum.labelOf(record.getStatus()));
        vo.setCreated(record.getCreated());
        vo.setModified(record.getModified());
        return vo;
    }

    private AuditDisableProductVO toDisableVO(AuditDisableProduct entity) {
        AuditDisableProductVO vo = new AuditDisableProductVO();
        vo.setId(entity.getId());
        vo.setPlatform(entity.getPlatform());
        AuditPlatform platform = AuditPlatform.fromCode(entity.getPlatform());
        vo.setPlatformLabel(platform == null ? entity.getPlatform() : platform.getLabel());
        vo.setProductId(entity.getProductId());
        vo.setProductName(entity.getProductName());
        return vo;
    }


    public Boolean startAudit(Long id) {
        AuditRecord auditRecord = auditRecordMapper.selectById(id);
        if (auditRecord == null) {
            return Boolean.TRUE;
        }
        auditPublisher.publishStartAudit(auditRecord);
        return Boolean.TRUE;
    }


    @Transactional(rollbackFor = Exception.class)
    public void startAudit(AuditRecord auditRecordDO) {
        //审核完成，不看
        if (AuditRecordStatusEnum.DONE.getCode().equals(auditRecordDO.getStatus())) {
            return;
        }
        LambdaQueryWrapper<AuditTradeRecord> queryWrapper = new LambdaQueryWrapper<AuditTradeRecord>()
                .eq(AuditTradeRecord::getAuditRecordId, auditRecordDO.getId());
        List<AuditTradeRecord> auditTradeRecordDOList = auditTradeRecordMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(auditTradeRecordDOList)) {
            log.info("数据未落库，auditRecordId:{}, accountNo:{}", auditRecordDO.getId(), auditRecordDO.getAccountNo());
            return;
        }

        //所有成交明细都已分析完毕，审核完成
        List<AuditTradeRecord> notDoneList = auditTradeRecordDOList.stream().filter(s -> AuditTradeRecordStatusEnum.INIT.getCode().equals(s.getStatus()) || AuditTradeRecordStatusEnum.DOING.getCode().equals(s.getStatus())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(notDoneList)) {
            auditRecordService.updateStatus(auditRecordDO.getId(), auditRecordDO.getStatus(), AuditRecordStatusEnum.DONE.getCode());
            return;
        }

        //完成中，分析未完成记录
        if (AuditRecordStatusEnum.DOING.getCode().equals(auditRecordDO.getStatus())) {
            CompletableFuture.runAsync(() -> batchExecuteFacade.analysis(notDoneList)).exceptionally(e -> {
                log.error("批量执行异常，auditId:{}", auditRecordDO.getId(), e);
                return null;
            });
            return;
        }

        boolean result = auditRecordService.updateStatus(auditRecordDO.getId(), auditRecordDO.getStatus(), AuditRecordStatusEnum.DOING.getCode());
        if (result) {
            CompletableFuture.runAsync(() -> batchExecuteFacade.analysis(notDoneList)).exceptionally(e -> {
                log.error("批量执行异常，auditId:{}", auditRecordDO.getId(), e);
                return null;
            });
        }
    }


    public void checkAuditDone(Long id) {
        AuditRecord auditRecordDO = auditRecordService.getById(id);
        if (AuditRecordStatusEnum.DONE.getCode().equals(auditRecordDO.getStatus())) {
            return;
        }

        Long count = auditTradeRecordService.countByStatus(
                auditRecordDO.getId(),
                Lists.newArrayList(AuditTradeRecordStatusEnum.PASS.getCode(), AuditTradeRecordStatusEnum.FAIL.getCode()));
        if (auditRecordDO.getTradeNum() == count.intValue()) {
            auditRecordService.updateStatus(id, auditRecordDO.getStatus(), AuditRecordStatusEnum.DONE.getCode());
        }
    }

}
