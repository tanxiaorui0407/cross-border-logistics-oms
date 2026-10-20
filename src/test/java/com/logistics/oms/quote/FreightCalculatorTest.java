package com.logistics.oms.quote;

import com.logistics.oms.module.channel.entity.Channel;
import com.logistics.oms.module.channel.entity.RateRule;
import com.logistics.oms.module.quote.core.FreightCalculator;
import com.logistics.oms.module.quote.strategy.ExpressFreightStrategy;
import com.logistics.oms.module.quote.strategy.FreightStrategyFactory;
import com.logistics.oms.module.quote.strategy.LineFreightStrategy;
import com.logistics.oms.module.quote.strategy.PostFreightStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 运费计算引擎测试：首重 / 续重 ceil / 燃油 / 挂号 / 体积重 / 专线系数 / BigDecimal 精度。
 */
class FreightCalculatorTest {

    private FreightCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new FreightCalculator(new FreightStrategyFactory(List.of(
                new ExpressFreightStrategy(), new PostFreightStrategy(), new LineFreightStrategy())));
    }

    private Channel channel(String type, Integer volDivisor) {
        Channel c = new Channel();
        c.setTransportType(type);
        c.setVolDivisor(volDivisor);
        return c;
    }

    private RateRule rule(BigDecimal firstW, BigDecimal firstP, BigDecimal stepW, BigDecimal stepP,
                          BigDecimal fuel, BigDecimal reg) {
        RateRule r = new RateRule();
        r.setFirstWeight(firstW);
        r.setFirstPrice(firstP);
        r.setStepWeight(stepW);
        r.setStepPrice(stepP);
        r.setFuelRate(fuel);
        r.setRegFee(reg);
        r.setEffectiveDate(LocalDate.now().minusDays(1));
        r.setExpiredDate(LocalDate.now().plusDays(1));
        return r;
    }

    @Test
    void withinFirstWeight_chargesFirstPriceOnly() {
        Channel c = channel("EXPRESS", 5000);
        RateRule r = rule(new BigDecimal("0.5"), new BigDecimal("10.00"),
                new BigDecimal("0.5"), new BigDecimal("5.00"),
                BigDecimal.ZERO, BigDecimal.ZERO);
        FreightCalculator.CalcResult res = calculator.calc(c, r, new BigDecimal("0.3"),
                null, null, null);
        assertEquals(0, new BigDecimal("10.00").compareTo(res.freight()));
    }

    @Test
    void stepWeightUsesCeiling() {
        Channel c = channel("EXPRESS", 5000);
        RateRule r = rule(new BigDecimal("0.5"), new BigDecimal("10.00"),
                new BigDecimal("0.5"), new BigDecimal("5.00"),
                BigDecimal.ZERO, BigDecimal.ZERO);
        // 2.3kg：首重0.5 + ceil(1.8/0.5)=4 个续重 → 10 + 4*5 = 30
        FreightCalculator.CalcResult res = calculator.calc(c, r, new BigDecimal("2.3"),
                null, null, null);
        assertEquals(0, new BigDecimal("30.00").compareTo(res.freight()));
        // 2.5kg：ceil(2.0/0.5)=4 仍是 30（不足一个续重单位按一个收）
        res = calculator.calc(c, r, new BigDecimal("2.5"), null, null, null);
        assertEquals(0, new BigDecimal("30.00").compareTo(res.freight()));
        // 2.6kg：ceil(2.1/0.5)=5 → 10+25=35
        res = calculator.calc(c, r, new BigDecimal("2.6"), null, null, null);
        assertEquals(0, new BigDecimal("35.00").compareTo(res.freight()));
    }

    @Test
    void fuelAndRegistrationFee() {
        Channel c = channel("EXPRESS", 5000);
        RateRule r = rule(new BigDecimal("0.5"), new BigDecimal("10.00"),
                new BigDecimal("0.5"), new BigDecimal("5.00"),
                new BigDecimal("10.00"), new BigDecimal("3.00"));
        // 30 * 1.10 + 3 = 36.00
        FreightCalculator.CalcResult res = calculator.calc(c, r, new BigDecimal("2.3"),
                null, null, null);
        assertEquals(0, new BigDecimal("36.00").compareTo(res.freight()));
    }

    @Test
    void volumeWeightOverridesActual() {
        Channel c = channel("EXPRESS", 5000);
        RateRule r = rule(new BigDecimal("0.5"), new BigDecimal("10.00"),
                new BigDecimal("0.5"), new BigDecimal("5.00"),
                new BigDecimal("10.00"), new BigDecimal("3.00"));
        // 50*40*40/5000 = 16kg > 实重1kg → 计费16kg
        FreightCalculator.CalcResult res = calculator.calc(c, r, new BigDecimal("1.0"),
                new BigDecimal("50"), new BigDecimal("40"), new BigDecimal("40"));
        assertEquals(0, new BigDecimal("16.000").compareTo(res.volWeight()));
        assertEquals(0, new BigDecimal("16.000").compareTo(res.billableWeight()));
        // 10 + ceil((16-0.5)/0.5)=31 * 5 = 165 → *1.10 = 181.50 + 3 = 184.50
        assertEquals(0, new BigDecimal("184.50").compareTo(res.freight()));
    }

    @Test
    void lineStrategyUsesDivisor8000() {
        Channel c = channel("LINE", 8000);
        RateRule r = rule(new BigDecimal("1.0"), new BigDecimal("25.00"),
                new BigDecimal("1.0"), new BigDecimal("10.00"),
                new BigDecimal("3.00"), new BigDecimal("8.00"));
        // 40*30*20/8000 = 3kg；计费3kg → 25 + 2*10 = 45 → *1.03 = 46.35 + 8 = 54.35
        FreightCalculator.CalcResult res = calculator.calc(c, r, new BigDecimal("1.5"),
                new BigDecimal("40"), new BigDecimal("30"), new BigDecimal("20"));
        assertEquals(0, new BigDecimal("3.000").compareTo(res.volWeight()));
        assertEquals(0, new BigDecimal("54.35").compareTo(res.freight()));
    }

    @Test
    void channelVolDivisorOverridesStrategyDefault() {
        // 渠道自定义系数 6000 覆盖策略默认
        Channel c = channel("EXPRESS", 6000);
        RateRule r = rule(new BigDecimal("0.5"), new BigDecimal("10.00"),
                new BigDecimal("0.5"), new BigDecimal("5.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        FreightCalculator.CalcResult res = calculator.calc(c, r, new BigDecimal("1.0"),
                new BigDecimal("60"), new BigDecimal("50"), new BigDecimal("40"));
        // 60*50*40/6000 = 20kg
        assertEquals(0, new BigDecimal("20.000").compareTo(res.volWeight()));
    }

    @Test
    void noDoublePrecisionLoss() {
        // 全程 BigDecimal：首重价 0.10 + 续重 0.20 这类边界不出现浮点误差
        Channel c = channel("EXPRESS", 5000);
        RateRule r = rule(new BigDecimal("0.1"), new BigDecimal("0.10"),
                new BigDecimal("0.1"), new BigDecimal("0.20"), BigDecimal.ZERO, BigDecimal.ZERO);
        FreightCalculator.CalcResult res = calculator.calc(c, r, new BigDecimal("0.5"),
                null, null, null);
        // 0.5 > 0.1：ceil(0.4/0.1)=4 → 0.10 + 4*0.20 = 0.90
        assertEquals("0.90", res.freight().toPlainString());
        assertNotNull(res.billableWeight());
        assertTrue(res.billableWeight().compareTo(BigDecimal.ZERO) > 0);
    }
}
