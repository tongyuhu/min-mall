package com.minimall.common.api;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PageQueryTest {
    @Test void rejectsOversizedPagesAndUnsafeSort() {
        assertThrows(BusinessException.class, () -> new PageQuery(1,101,"id"));
        assertThrows(BusinessException.class, () -> new PageQuery(1,10,"id;drop table member"));
        assertEquals(20L, new PageQuery(3,10,"id").offset());
    }
}
