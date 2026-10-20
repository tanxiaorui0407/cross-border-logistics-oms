package com.logistics.oms.module.bill.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.logistics.oms.common.BusinessException;
import com.logistics.oms.common.PageResult;
import com.logistics.oms.module.bill.dto.BillDetailVO;
import com.logistics.oms.module.bill.dto.BillExportRow;
import com.logistics.oms.module.bill.dto.BillVO;
import com.logistics.oms.module.bill.entity.Bill;
import com.logistics.oms.module.bill.entity.BillDetail;
import com.logistics.oms.module.bill.mapper.BillDetailMapper;
import com.logistics.oms.module.bill.mapper.BillMapper;
import com.logistics.oms.module.customer.entity.Customer;
import com.logistics.oms.module.customer.mapper.CustomerMapper;
import com.logistics.oms.module.order.entity.OrderMain;
import com.logistics.oms.module.order.enums.OrderStatus;
import com.logistics.oms.module.order.mapper.OrderMainMapper;
import com.logistics.oms.security.LoginUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 账单：按月为每个客户聚合有效订单（排除 DRAFT/CANCELED/REJECTED），
 * 生成账单主表 + 明细，支持 Excel 导出。
 */
@Service
public class BillService {

    private final BillMapper billMapper;
    private final BillDetailMapper detailMapper;
    private final CustomerMapper customerMapper;
    private final OrderMainMapper orderMapper;

    public BillService(BillMapper billMapper, BillDetailMapper detailMapper,
                       CustomerMapper customerMapper, OrderMainMapper orderMapper) {
        this.billMapper = billMapper;
        this.detailMapper = detailMapper;
        this.customerMapper = customerMapper;
        this.orderMapper = orderMapper;
    }

    @Transactional
    public BillVO generate(Long customerId, String period, LoginUser user) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw BusinessException.notFound("客户不存在");
        }
        if (!isValidPeriod(period)) {
            throw BusinessException.param("账期格式应为 yyyy-MM");
        }
        long exists = billMapper.selectCount(new QueryWrapper<Bill>()
                .eq("customer_id", customerId)
                .eq("period", period));
        if (exists > 0) {
            throw BusinessException.conflict("该客户该账期账单已生成");
        }

        LocalDate start = LocalDate.parse(period + "-01");
        LocalDate end = start.plusMonths(1);
        List<OrderMain> orders = orderMapper.selectList(new QueryWrapper<OrderMain>()
                .eq("customer_id", customerId)
                .ge("created_at", start.atStartOfDay())
                .lt("created_at", end.atStartOfDay())
                .notIn("status", OrderStatus.DRAFT.name(),
                        OrderStatus.CANCELED.name(), OrderStatus.REJECTED.name())
                .orderByAsc("order_no"));
        if (orders.isEmpty()) {
            throw BusinessException.param("该账期无有效订单，无需生成账单");
        }

        BigDecimal total = orders.stream()
                .map(OrderMain::getFreight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Bill bill = new Bill();
        bill.setBillNo("BI" + period.replace("-", "") + "-" + customerId);
        bill.setCustomerId(customerId);
        bill.setPeriod(period);
        bill.setTotalAmount(total.setScale(2, BigDecimal.ROUND_HALF_UP));
        bill.setCurrency("USD");
        bill.setStatus("OPEN");
        billMapper.insert(bill);

        for (OrderMain order : orders) {
            BillDetail detail = new BillDetail();
            detail.setBillId(bill.getId());
            detail.setOrderId(order.getId());
            detail.setOrderNo(order.getOrderNo());
            detail.setGoodsDesc(order.getGoodsDesc());
            detail.setFreight(order.getFreight());
            detail.setCurrency(order.getCurrency());
            detailMapper.insert(detail);
        }
        return detail(bill.getId(), user);
    }

    public PageResult<BillVO> page(long current, long size, Long customerId, String period, LoginUser user) {
        QueryWrapper<Bill> w = new QueryWrapper<>();
        if ("CUSTOMER".equals(user.role())) {
            w.eq("customer_id", user.customerId());
        } else if (customerId != null) {
            w.eq("customer_id", customerId);
        }
        if (period != null && !period.isBlank()) {
            w.eq("period", period);
        }
        w.orderByDesc("period").orderByDesc("id");
        Page<Bill> p = billMapper.selectPage(new Page<>(current, size), w);
        Set<Long> customerIds = p.getRecords().stream().map(Bill::getCustomerId).collect(Collectors.toSet());
        Map<Long, Customer> customers = customerIds.isEmpty() ? Map.of()
                : customerMapper.selectBatchIds(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));
        Map<Long, Long> orderCounts = orderCounts(p.getRecords().stream().map(Bill::getId).collect(Collectors.toList()));
        return PageResult.of(p, bill -> toVO(bill, customers, orderCounts, false));
    }

    public BillVO detail(Long id, LoginUser user) {
        Bill bill = getVisible(id, user);
        List<BillDetail> details = detailMapper.selectList(new QueryWrapper<BillDetail>()
                .eq("bill_id", id)
                .orderByAsc("order_no"));
        Map<Long, Customer> customers = Map.of(bill.getCustomerId(),
                customerMapper.selectById(bill.getCustomerId()));
        BillVO vo = toVO(bill, customers, Map.of(), true);
        vo.setDetails(details.stream().map(d -> {
            BillDetailVO dv = new BillDetailVO();
            dv.setOrderId(d.getOrderId());
            dv.setOrderNo(d.getOrderNo());
            dv.setGoodsDesc(d.getGoodsDesc());
            dv.setFreight(d.getFreight());
            dv.setCurrency(d.getCurrency());
            return dv;
        }).collect(Collectors.toList()));
        return vo;
    }

    public void markPaid(Long id, LoginUser user) {
        Bill bill = getVisible(id, user);
        bill.setStatus("PAID");
        billMapper.updateById(bill);
    }

    public byte[] export(Long id, LoginUser user) {
        BillVO vo = detail(id, user);
        Map<Long, OrderMain> orders = orderMapper.selectBatchIds(
                vo.getDetails().stream().map(BillDetailVO::getOrderId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(OrderMain::getId, Function.identity()));
        List<BillExportRow> rows = vo.getDetails().stream().map(d -> {
            BillExportRow r = new BillExportRow();
            r.setOrderNo(d.getOrderNo());
            r.setGoodsDesc(d.getGoodsDesc());
            r.setBillableWeight(orders.containsKey(d.getOrderId()) ? orders.get(d.getOrderId()).getBillableWeight() : null);
            r.setFreight(d.getFreight());
            r.setCurrency(d.getCurrency());
            return r;
        }).collect(Collectors.toList());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out, BillExportRow.class).sheet("账单明细").doWrite(rows);
        return out.toByteArray();
    }

    // ---------- 内部方法 ----------

    private Bill getVisible(Long id, LoginUser user) {
        QueryWrapper<Bill> w = new QueryWrapper<Bill>().eq("id", id);
        if ("CUSTOMER".equals(user.role())) {
            w.eq("customer_id", user.customerId());
        }
        Bill bill = billMapper.selectOne(w);
        if (bill == null) {
            throw BusinessException.notFound("账单不存在或无权访问");
        }
        return bill;
    }

    private BillVO toVO(Bill bill, Map<Long, Customer> customers, Map<Long, Long> orderCounts, boolean withCount) {
        BillVO vo = new BillVO();
        vo.setId(bill.getId());
        vo.setBillNo(bill.getBillNo());
        vo.setCustomerId(bill.getCustomerId());
        Customer customer = customers.get(bill.getCustomerId());
        vo.setCustomerName(customer == null ? null : customer.getCompanyName());
        vo.setPeriod(bill.getPeriod());
        vo.setTotalAmount(bill.getTotalAmount());
        vo.setCurrency(bill.getCurrency());
        vo.setStatus(bill.getStatus());
        vo.setCreatedAt(bill.getCreatedAt());
        if (withCount) {
            vo.setOrderCount(orderCounts.getOrDefault(bill.getId(), 0L).intValue());
        }
        return vo;
    }

    private Map<Long, Long> orderCounts(List<Long> billIds) {
        if (billIds.isEmpty()) {
            return Map.of();
        }
        List<BillDetail> details = detailMapper.selectList(new QueryWrapper<BillDetail>()
                .in("bill_id", billIds));
        return details.stream().collect(Collectors.groupingBy(
                BillDetail::getBillId, Collectors.counting()));
    }

    private boolean isValidPeriod(String period) {
        try {
            LocalDate.parse(period + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
