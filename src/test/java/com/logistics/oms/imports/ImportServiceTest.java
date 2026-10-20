package com.logistics.oms.imports;

import com.alibaba.excel.EasyExcel;
import com.logistics.oms.module.channel.entity.Channel;
import com.logistics.oms.module.channel.entity.RateRule;
import com.logistics.oms.module.channel.service.ChannelService;
import com.logistics.oms.module.channel.service.RateRuleService;
import com.logistics.oms.module.common.service.OrderNoGenerator;
import com.logistics.oms.module.customer.entity.Customer;
import com.logistics.oms.module.customer.mapper.CustomerMapper;
import com.logistics.oms.module.imports.dto.ImportResultVO;
import com.logistics.oms.module.imports.dto.OrderImportRow;
import com.logistics.oms.module.imports.entity.ImportRecord;
import com.logistics.oms.module.imports.mapper.ImportErrorMapper;
import com.logistics.oms.module.imports.mapper.ImportRecordMapper;
import com.logistics.oms.module.imports.service.ImportService;
import com.logistics.oms.module.order.entity.OrderMain;
import com.logistics.oms.module.order.mapper.OrderAddressMapper;
import com.logistics.oms.module.order.mapper.OrderMainMapper;
import com.logistics.oms.module.order.service.TrackService;
import com.logistics.oms.module.quote.core.FreightCalculator;
import com.logistics.oms.security.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Excel 导入测试：行级校验、错误报告、成功行落库为 SUBMITTED。
 */
class ImportServiceTest {

    private OrderMainMapper orderMapper;
    private OrderAddressMapper addressMapper;
    private CustomerMapper customerMapper;
    private ChannelService channelService;
    private RateRuleService rateRuleService;
    private FreightCalculator freightCalculator;
    private TrackService trackService;
    private OrderNoGenerator orderNoGenerator;
    private ImportRecordMapper recordMapper;
    private ImportErrorMapper errorMapper;
    private ImportService importService;

    @BeforeEach
    void setUp() {
        orderMapper = mock(OrderMainMapper.class);
        addressMapper = mock(OrderAddressMapper.class);
        customerMapper = mock(CustomerMapper.class);
        channelService = mock(ChannelService.class);
        rateRuleService = mock(RateRuleService.class);
        freightCalculator = mock(FreightCalculator.class);
        trackService = mock(TrackService.class);
        orderNoGenerator = mock(OrderNoGenerator.class);
        recordMapper = mock(ImportRecordMapper.class);
        errorMapper = mock(ImportErrorMapper.class);
        importService = new ImportService(orderMapper, addressMapper, customerMapper,
                channelService, rateRuleService, freightCalculator, trackService,
                orderNoGenerator, recordMapper, errorMapper);

        Channel channel = new Channel();
        channel.setId(1L);
        channel.setChannelCode("EXPRESS");
        channel.setChannelName("国际快递");
        channel.setCurrency("USD");
        when(channelService.listActive()).thenReturn(List.of(channel));

        RateRule rule = new RateRule();
        rule.setId(1L);
        rule.setChannelId(1L);
        rule.setDestCountry("US");
        when(rateRuleService.findEffective(1L, "US")).thenReturn(rule);

        Customer customer = new Customer();
        customer.setId(1L);
        customer.setStatus(1);
        when(customerMapper.selectOne(any())).thenReturn(customer);

        when(freightCalculator.calc(any(), any(), any(), any(), any(), any()))
                .thenReturn(new FreightCalculator.CalcResult(new BigDecimal("0.600"),
                        new BigDecimal("0.600"), new BigDecimal("27.30")));
        when(orderNoGenerator.nextOrderNo()).thenReturn("SO202610210001");
        doAnswer(inv -> {
            OrderMain o = inv.getArgument(0);
            o.setId(1L);
            return 1;
        }).when(orderMapper).insert(any());
        doAnswer(inv -> {
            ImportRecord r = inv.getArgument(0);
            r.setId(99L);
            return 1;
        }).when(recordMapper).insert(any());
    }

    private OrderImportRow row(String goodsDesc) {
        OrderImportRow r = new OrderImportRow();
        r.setCustomerCode("CUST001");
        r.setGoodsDesc(goodsDesc);
        r.setPieces("1");
        r.setActualWeight("0.5");
        r.setDestCountry("US");
        r.setChannelCode("EXPRESS");
        r.setSenderName("Alice");
        r.setReceiverName("John");
        r.setReceiverCountry("US");
        return r;
    }

    private MockMultipartFile toXlsx(List<OrderImportRow> rows) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out, OrderImportRow.class).sheet("订单导入").doWrite(rows);
        return new MockMultipartFile("file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
    }

    @Test
    void importOrders_validAndInvalidRows() {
        // 第 2 行有效；第 3 行缺品名 → 行级错误
        ImportResultVO result = importService.importOrders(
                toXlsx(List.of(row("蓝牙耳机"), row(""))),
                new LoginUser(10L, "ops", "OPS", null));

        assertEquals(2, result.getTotal());
        assertEquals(1, result.getSuccess());
        assertEquals(1, result.getFailed());
        assertEquals(3, result.getErrors().get(0).getRowNo()); // 表头占第 1 行

        ArgumentCaptor<OrderMain> captor = ArgumentCaptor.forClass(OrderMain.class);
        verify(orderMapper).insert(captor.capture());
        assertEquals("SUBMITTED", captor.getValue().getStatus());
        assertEquals("SO202610210001", captor.getValue().getOrderNo());
        verify(errorMapper).insert(any());
    }

    @Test
    void importOrders_allValidNoErrors() {
        ImportResultVO result = importService.importOrders(
                toXlsx(List.of(row("蓝牙耳机"), row("充电宝"))),
                new LoginUser(10L, "ops", "OPS", null));

        assertEquals(2, result.getSuccess());
        assertEquals(0, result.getFailed());
        assertEquals(0, result.getErrors().size());
        verify(errorMapper, org.mockito.Mockito.never()).insert(any());
    }

    @Test
    void importOrders_customerRoleIgnoresSheetCustomerCode() {
        ImportResultVO result = importService.importOrders(
                toXlsx(List.of(row("蓝牙耳机"))),
                new LoginUser(11L, "alice", "CUSTOMER", 1L));

        assertEquals(1, result.getSuccess());
        // CUSTOMER 角色不应通过客户编码查询
        verify(customerMapper, org.mockito.Mockito.never()).selectOne(any());
        assertNotNull(result.getRecordId());
    }

    @Test
    void importOrders_customerRoleWithoutCustomerIdRejected() {
        ImportResultVO result = importService.importOrders(
                toXlsx(List.of(row("蓝牙耳机"))),
                new LoginUser(12L, "solo", "CUSTOMER", null));

        assertEquals(0, result.getSuccess());
        assertEquals(1, result.getFailed());
    }
}
