package com.minimall.common.persistence;
import java.util.Map;
/** 仅接受仓库代码中的SQL常量；值始终通过MyBatis预编译参数绑定。 */
public final class SqlStatements {
    private SqlStatements(){}
    public static String statement(Map<String,Object> parameters){
        String sql=(String)parameters.get("sql"); StringBuilder result=new StringBuilder();int index=0;
        for(int i=0;i<sql.length();i++){char c=sql.charAt(i);if(c=='?')result.append("#{args[").append(index++).append("]}");else result.append(c);}
        if(index!=((Object[])parameters.get("args")).length)throw new IllegalArgumentException("SQL参数数目不匹配");
        return result.toString();
    }
}
