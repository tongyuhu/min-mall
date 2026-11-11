package com.minimall.app;

import static org.junit.jupiter.api.Assertions.*;

import com.minimall.app.support.IntegrationTestSupport;
import com.minimall.common.api.BusinessException;
import com.minimall.common.api.PageQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;

class ApiContractIT extends IntegrationTestSupport {
  @Autowired ObjectMapper mapper;

  record Example(String id, long amount) {}

  @Test
  void preservesLargeIdWithoutChangingMoneyType() {
    var tree = mapper.readTree(mapper.writeValueAsString(new Example("9007199254740993", 12345)));
    assertTrue(tree.get("id").isString());
    assertEquals("9007199254740993", tree.get("id").asString());
    assertEquals(12345L, tree.get("amount").asLong());
    assertThrows(BusinessException.class, () -> new PageQuery(1, 101, "id"));
  }
}
