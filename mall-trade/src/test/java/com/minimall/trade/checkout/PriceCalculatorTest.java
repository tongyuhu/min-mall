package com.minimall.trade.checkout;
import com.minimall.trade.checkout.domain.PriceCalculator;import com.minimall.common.api.BusinessException;import org.junit.jupiter.api.Test;import java.util.List;import static org.junit.jupiter.api.Assertions.*;
class PriceCalculatorTest {
 @Test void distributesRemainderWithoutLosingMoney(){var result=PriceCalculator.calculate(List.of(100L,100L,100L),1,0);assertEquals(List.of(99L,100L,100L),result.linePayableFen());assertEquals(299,result.payableFen());}
 @Test void keepsFreightWhenGoodsAreFree(){var result=PriceCalculator.calculate(List.of(100L),100,20);assertEquals(20,result.payableFen());assertEquals(List.of(0L),result.linePayableFen());}
 @Test void rejectsOverflow(){assertThrows(BusinessException.class,()->PriceCalculator.calculate(List.of(Long.MAX_VALUE,1L),0,0));}
 @Test void neverDiscountsZeroValueLine(){assertEquals(List.of(0L,0L,1L),PriceCalculator.calculate(List.of(0L,1L,1L),1,0).linePayableFen());}
}
