package com.minimall.common.persistence;
import com.minimall.common.api.BusinessException;
import org.springframework.stereotype.Component; import java.util.*; import java.time.Instant; import java.sql.Timestamp;
@Component public class Database {
    private final SqlMapper mapper; public Database(SqlMapper mapper){this.mapper=mapper;}
    private Map<String,Object> parameters(String sql,Object... args){var result=new HashMap<String,Object>();result.put("sql",sql);result.put("args",Arrays.stream(args).map(v->v instanceof Instant i?Timestamp.from(i):v).toArray());return result;}
    public List<DbRow> rows(String sql,Object... args){return mapper.select(parameters(sql,args)).stream().map(DbRow::new).toList();}
    public Optional<DbRow> first(String sql,Object... args){return rows(sql,args).stream().findFirst();}
    public DbRow required(String sql,Object... args){return first(sql,args).orElseThrow(BusinessException::missing);}
    public int update(String sql,Object... args){return mapper.update(parameters(sql,args));}
    public long insert(String sql,Object... args){var p=parameters(sql,args);mapper.insert(p);return ((Number)p.get("generatedId")).longValue();}
    public long count(String sql,Object... args){var row=required(sql,args);return row.number("value");}
}
