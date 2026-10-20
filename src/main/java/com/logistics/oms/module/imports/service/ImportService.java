package com.logistics.oms.module.imports.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.logistics.oms.common.BusinessException;
import com.logistics.oms.module.channel.entity.Channel;
import com.logistics.oms.module.channel.entity.RateRule;
import com.logistics.oms.module.channel.service.ChannelService;
import com.logistics.oms.module.channel.service.RateRuleService;
import com.logistics.oms.module.common.service.OrderNoGenerator;
import com.logistics.oms.module.customer.entity.Customer;
import com.logistics.oms.module.customer.mapper.CustomerMapper;
import com.logistics.oms.module.imports.dto.ImportErrorRow;
import com.logistics.oms.module.imports.dto.ImportResultVO;
import com.logistics.oms.module.imports.dto.OrderImportRow;
import com.logistics.oms.module.imports.entity.ImportError;
import com.logistics.oms.module.imports.entity.ImportRecord;
import com.logistics.oms.module.imports.mapper.ImportErrorMapper;
import com.logistics.oms.module.imports.mapper.ImportRecordMapper;
import com.logistics.oms.module.order.entity.OrderAddress;
import com.logistics.oms.module.order.entity.OrderMain;
import com.logistics.oms.module.order.enums.OrderStatus;
import com.logistics.oms.module.order.mapper.OrderAddressMapper;
import com.logistics.oms.module.order.mapper.OrderMainMapper;
import com.logistics.oms.module.order.service.TrackService;
import com.logistics.oms.module.quote.core.FreightCalculator;
import com.logistics.oms.security.LoginUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Excel 批量导入：行级校验 → 成功行落库（状态 SUBMITTED）→ 失败行生成错误报告并留痕。
 */
@Service
public class ImportService {

    private final OrderMainMapper orderMapper;
    private final OrderAddressMapper addressMapper;
    private final CustomerMapper customerMapper;
    private final ChannelService channelService;
    private final RateRuleService rateRuleService;
    private final FreightCalculator freightCalculator;
    private final TrackService trackService;
    private final OrderNoGenerator orderNoGenerator;
    private final ImportRecordMapper recordMapper;
    private final ImportErrorMapper errorMapper;

    public ImportService(OrderMainMapper orderMapper, OrderAddressMapper addressMapper,
                         CustomerMapper customerMapper, ChannelService channelService,
                         RateRuleService rateRuleService, FreightCalculator freightCalculator,
                         TrackService trackService, OrderNoGenerator orderNoGenerator,
                         ImportRecordMapper recordMapper, ImportErrorMapper errorMapper) {
        this.orderMapper = orderMapper;
        this.addressMapper = addressMapper;
        this.customerMapper = customerMapper;
        this.channelService = channelService;
        this.rateRuleService = rateRuleService;
        this.freightCalculator = freightCalculator;
        this.trackService = trackService;
        this.orderNoGenerator = orderNoGenerator;
        this.recordMapper = recordMapper;
        this.errorMapper = errorMapper;
    }

    @Transactional
    public ImportResultVO importOrders(MultipartFile file, LoginUser user) {
        List<OrderImportRow> rows = readRows(file);

        ImportRecord record = new ImportRecord();
        record.setFileName(file.getOriginalFilename());
        record.setOperator(user.username());
        record.setTotal(rows.size());
        record.setSuccess(0);
        record.setFailed(0);
        recordMapper.insert(record);

        int success = 0;
        List<ImportResultVO.ImportErrorVO> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNo = i + 2; // 表头占第 1 行
            try {
                createOrderFromRow(rows.get(i), user);
                success++;
            } catch (BusinessException e) {
                errors.add(new ImportResultVO.ImportErrorVO(rowNo, e.getMessage()));
            } catch (Exception e) {
                errors.add(new ImportResultVO.ImportErrorVO(rowNo, "系统处理异常"));
            }
        }

        if (!errors.isEmpty()) {
            for (ImportResultVO.ImportErrorVO err : errors) {
                ImportError ie = new ImportError();
                ie.setRecordId(record.getId());
                ie.setRowNo(err.getRowNo());
                ie.setErrorMsg(err.getErrorMsg());
                errorMapper.insert(ie);
            }
        }

        record.setSuccess(success);
        record.setFailed(errors.size());
        recordMapper.updateById(record);

        ImportResultVO vo = new ImportResultVO();
        vo.setRecordId(record.getId());
        vo.setFileName(record.getFileName());
        vo.setTotal(record.getTotal());
        vo.setSuccess(success);
        vo.setFailed(errors.size());
        vo.setErrors(errors);
        return vo;
    }

    /** 导入模板（仅表头）。 */
    public byte[] templateBytes() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out, OrderImportRow.class).sheet("订单导入").doWrite(List.of());
        return out.toByteArray();
    }

    /** 错误报告导出。 */
    public byte[] errorReportBytes(Long recordId) {
        List<ImportError> list = errorMapper.selectList(new QueryWrapper<ImportError>()
                .eq("record_id", recordId)
                .orderByAsc("row_no"));
        if (list.isEmpty()) {
            throw BusinessException.param("该导入记录无错误");
        }
        List<ImportErrorRow> rows = list.stream()
                .map(e -> new ImportErrorRow(e.getRowNo(), e.getErrorMsg()))
                .collect(Collectors.toList());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out, ImportErrorRow.class).sheet("错误报告").doWrite(rows);
        return out.toByteArray();
    }

    // ---------- 内部实现 ----------

    private List<OrderImportRow> readRows(MultipartFile file) {
        List<OrderImportRow> rows;
        try (InputStream in = file.getInputStream()) {
            rows = EasyExcel.read(in).head(OrderImportRow.class).sheet(0).doReadSync();
        } catch (Exception e) {
            throw BusinessException.param("文件解析失败，请下载模板按格式填写");
        }
        if (rows == null) {
            return List.of();
        }
        // 跳过全空行
        return rows.stream().filter(r -> StringUtils.hasText(r.getGoodsDesc())
                        || StringUtils.hasText(r.getActualWeight())
                        || StringUtils.hasText(r.getChannelCode()))
                .collect(Collectors.toList());
    }

    private void createOrderFromRow(OrderImportRow row, LoginUser user) {
        // 基础必填校验
        requireText(row.getGoodsDesc(), "品名不能为空");
        requireText(row.getDestCountry(), "目的国不能为空");
        requireText(row.getChannelCode(), "渠道编码不能为空");
        BigDecimal actualWeight = parseDecimal(row.getActualWeight(), "实重");
        if (actualWeight == null || actualWeight.compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.param("实重必须大于0");
        }
        requireText(row.getSenderName(), "寄件人不能为空");
        requireText(row.getReceiverName(), "收件人不能为空");
        requireText(row.getReceiverCountry(), "收件人国家不能为空");

        Channel channel = channelService.listActive().stream()
                .filter(c -> c.getChannelCode().equalsIgnoreCase(row.getChannelCode().trim()))
                .findFirst()
                .orElseThrow(() -> BusinessException.param("渠道编码不存在或已停用: " + row.getChannelCode()));

        RateRule rule = rateRuleService.findEffective(channel.getId(), row.getDestCountry().trim());
        if (rule == null) {
            throw BusinessException.param("渠道[" + channel.getChannelName() + "]暂无目的国["
                    + row.getDestCountry() + "]价卡");
        }

        Long customerId = resolveCustomer(row.getCustomerCode(), user);

        BigDecimal length = parseDecimal(row.getLengthCm(), "长");
        BigDecimal width = parseDecimal(row.getWidthCm(), "宽");
        BigDecimal height = parseDecimal(row.getHeightCm(), "高");
        FreightCalculator.CalcResult calc = freightCalculator.calc(
                channel, rule, actualWeight, length, width, height);

        OrderMain order = new OrderMain();
        order.setOrderNo(orderNoGenerator.nextOrderNo());
        order.setCustomerId(customerId);
        order.setChannelId(channel.getId());
        order.setStatus(OrderStatus.SUBMITTED.name()); // 导入即视为客户提交
        order.setVersion(0);
        order.setGoodsDesc(row.getGoodsDesc().trim());
        order.setPieces(parseInt(row.getPieces(), 1));
        order.setActualWeight(actualWeight);
        order.setLengthCm(length);
        order.setWidthCm(width);
        order.setHeightCm(height);
        order.setVolWeight(calc.volWeight());
        order.setBillableWeight(calc.billableWeight());
        order.setFreight(calc.freight());
        order.setCurrency(channel.getCurrency() == null ? "USD" : channel.getCurrency());
        orderMapper.insert(order);

        insertAddress(order.getId(), "SENDER",
                row.getSenderName(), row.getSenderPhone(), "CN",
                row.getSenderCity(), row.getSenderStreet(), row.getSenderZip());
        insertAddress(order.getId(), "RECEIVER",
                row.getReceiverName(), row.getReceiverPhone(), row.getReceiverCountry(),
                row.getReceiverCity(), row.getReceiverStreet(), row.getReceiverZip());
        trackService.addSystemEvent(order.getId(), OrderStatus.SUBMITTED.name(),
                "批量导入创建并提交", user.username());
    }

    private Long resolveCustomer(String customerCode, LoginUser user) {
        if ("CUSTOMER".equals(user.role())) {
            if (user.customerId() == null) {
                throw BusinessException.forbidden("当前账号未关联客户");
            }
            return user.customerId();
        }
        if (!StringUtils.hasText(customerCode)) {
            throw BusinessException.param("客户编码不能为空");
        }
        Customer customer = customerMapper.selectOne(new QueryWrapper<Customer>()
                .eq("customer_code", customerCode.trim()));
        if (customer == null || customer.getStatus() == null || customer.getStatus() != 1) {
            throw BusinessException.param("客户编码不存在或已停用: " + customerCode);
        }
        return customer.getId();
    }

    private void insertAddress(Long orderId, String type, String name, String phone,
                               String country, String city, String street, String zip) {
        OrderAddress addr = new OrderAddress();
        addr.setOrderId(orderId);
        addr.setAddrType(type);
        addr.setName(name == null ? "" : name.trim());
        addr.setPhone(phone == null ? null : phone.trim());
        addr.setCountry(country == null ? "" : country.trim().toUpperCase());
        addr.setCity(city == null ? null : city.trim());
        addr.setStreet(street == null ? null : street.trim());
        addr.setZip(zip == null ? null : zip.trim());
        addressMapper.insert(addr);
    }

    private void requireText(String s, String msg) {
        if (!StringUtils.hasText(s)) {
            throw BusinessException.param(msg);
        }
    }

    private BigDecimal parseDecimal(String s, String field) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            throw BusinessException.param(field + "格式错误: " + s);
        }
    }

    private int parseInt(String s, int defaultValue) {
        if (!StringUtils.hasText(s)) {
            return defaultValue;
        }
        try {
            int v = Integer.parseInt(s.trim());
            return v < 1 ? defaultValue : v;
        } catch (NumberFormatException e) {
            throw BusinessException.param("件数格式错误: " + s);
        }
    }
}
