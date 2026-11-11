package com.minimall.common.persistence;

import java.util.*;
import org.apache.ibatis.annotations.*;

@Mapper
public interface SqlMapper {
  @SelectProvider(type = SqlStatements.class, method = "statement")
  List<Map<String, Object>> select(Map<String, Object> parameters);

  @UpdateProvider(type = SqlStatements.class, method = "statement")
  int update(Map<String, Object> parameters);

  @InsertProvider(type = SqlStatements.class, method = "statement")
  @Options(useGeneratedKeys = true, keyProperty = "generatedId")
  int insert(Map<String, Object> parameters);
}
