package com.logistics.oms.bill;

import com.logistics.oms.common.BusinessException;
import com.logistics.oms.module.bill.dto.BillVO;
import com.logistics.oms.module.bill.entity.Bill;
import com.logistics.oms.module.bill.entity.BillDetail;
import com.logistics.oms.module.bill.mapper.BillDetailMapper;
import com.logistics.oms.module.bill.mapper.BillMapper;
import com.logistics.oms.module.bill.service.BillService;
import com.logistics.oms.module.customer.entity.Customer;
import com.logistics.oms.module.customer.mapper.CustomerMapper;
import com.logistics.oms.module.order.entity.OrderMain;
import com.logistics.oms.module.order.mapper.OrderMainMapper;
import com.logistics.oms.security.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 账单服务测试：按月聚合、重复生成拦截、无订单拦截、明细导出数据。
 */
class BillServiceTest {

    private BillMapper billMapper;
    private BillDetailMapper detailMapper;
    private CustomerMapper customerMapper;
    private OrderMainMapper orderMapper;
    private BillService billService;

    private LoginUser ops = new LoginUser(10L, "ops", "OPS", null);

    @BeforeEach
    void setUp() {
        billMapper = mock(BillMapper.class);
        detailMapper = mock(BillDetailMapper.class);
        customerMapper = mock(CustomerMapper.class);
        orderMapper = mock(OrderMainMapper.class);
        billService = new BillService(billMapper, detailMapper, customerMapper, orderMapper);

        Customer customer = new Customer();
        customer.setId(1L);
        customer.setCompanyName("深圳飞鱼贸易有限公司");
        when(customerMapper.selectById(1L)).thenReturn(customer);
        when(billMapper.selectCount(any())).thenReturn(0L);
    }

    private OrderMain order(Long id, String no, String freight) {
        OrderMain o = new OrderMain();
        o.setId(id);
        o.setOrderNo(no);
        o.setGoodsDesc("蓝牙耳机");
        o.setFreight(new BigDecimal(freight));
        o.setCurrency("USD");
        o.setCreatedAt(LocalDateTime.now());
        return o;
    }

    @Test
    void generate_aggregatesOrdersOfPeriod() {
        when(orderMapper.selectList(any())).thenReturn(List.of(
                order(1L, "SO202610180001", "27.30"),
                order(2L, "SO202610180002", "8.90")));
        doAnswer(inv -> {
            Bill b = inv.getArgument(0);
            b.setId(100L);
            return 1;
        }).when(billMapper).insert(any());
        when(detailMapper.insert(any())).thenReturn(1);

        // generate() 末尾调用 detail()：
        Bill bill = new Bill();
        bill.setId(100L);
        bill.setBillNo("BI202610-1");
        bill.setCustomerId(1L);
        bill.setPeriod("2026-10");
        bill.setTotalAmount(new BigDecimal("36.20"));
        bill.setStatus("OPEN");
        when(billMapper.selectOne(any())).thenReturn(bill);
        BillDetail d1 = new BillDetail();
        d1.setOrderId(1L);
        d1.setOrderNo("SO202610180001");
        d1.setFreight(new BigDecimal("27.30"));
        BillDetail d2 = new BillDetail();
        d2.setOrderId(2L);
        d2.setOrderNo("SO202610180002");
        d2.setFreight(new BigDecimal("8.90"));
        when(detailMapper.selectList(any())).thenReturn(List.of(d1, d2));

        BillVO vo = billService.generate(1L, "2026-10", ops);

        assertEquals("BI202610-1", vo.getBillNo());
        assertEquals(0, new BigDecimal("36.20").compareTo(vo.getTotalAmount()));
        assertEquals("OPEN", vo.getStatus());
        assertEquals(2, vo.getDetails().size());
        verify(detailMapper, times(2)).insert(any());
    }

    @Test
    void generate_duplicatePeriodRejected() {
        when(billMapper.selectCount(any())).thenReturn(1L);
        BusinessException e = assertThrows(BusinessException.class,
                () -> billService.generate(1L, "2026-10", ops));
        assertEquals(409, e.getCode());
    }

    @Test
    void generate_noValidOrdersRejected() {
        when(orderMapper.selectList(any())).thenReturn(List.of());
        BusinessException e = assertThrows(BusinessException.class,
                () -> billService.generate(1L, "2026-10", ops));
        assertEquals(400, e.getCode());
    }

    @Test
    void generate_invalidPeriodRejected() {
        assertThrows(BusinessException.class, () -> billService.generate(1L, "2026-13", ops));
    }
}
