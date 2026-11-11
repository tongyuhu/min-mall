package com.minimall.common.api;
import java.util.Set;
public record PageQuery(int page,int size,String sort) {
    public PageQuery { if(page<1 || page>1000000 || size<1 || size>100)throw BusinessException.bad("分页参数超出范围");
        if(sort==null)sort="id"; if(!Set.of("id","price","createdAt").contains(sort))throw BusinessException.bad("不支持的排序字段"); }
    public long offset(){return (long)(page-1)*size;}
}
