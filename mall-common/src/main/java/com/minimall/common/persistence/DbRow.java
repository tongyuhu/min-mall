package com.minimall.common.persistence;
import java.util.*; import java.time.*; import java.sql.Timestamp;
public final class DbRow {
    private final Map<String,Object> values=new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    public DbRow(Map<String,Object> values){this.values.putAll(values);}
    public Object value(String key){return values.get(key);}
    public String text(String key){Object v=value(key);return v==null?null:v.toString();}
    public long number(String key){Object v=value(key);return v==null?0:((Number)v).longValue();}
    public Long optionalNumber(String key){return value(key)==null?null:number(key);}
    public int integer(String key){return Math.toIntExact(number(key));}
    public boolean flag(String key){Object v=value(key);return v instanceof Boolean b?b:v!=null&&((Number)v).intValue()!=0;}
    public Instant instant(String key){Object v=value(key);if(v==null)return null;if(v instanceof Instant i)return i;if(v instanceof Timestamp t)return t.toInstant();if(v instanceof LocalDateTime l)return l.toInstant(ZoneOffset.UTC);throw new IllegalArgumentException("无法解析时间字段");}
}
