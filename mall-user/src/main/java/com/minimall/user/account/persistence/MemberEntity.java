package com.minimall.user.account.persistence;

import com.baomidou.mybatisplus.annotation.*;

@TableName("member")
public class MemberEntity {
  @TableId(type = IdType.AUTO)
  public Long id;

  public String username;
  public String passwordHash;
  public String nickname;
  public Boolean enabled;
  public Long authVersion;
}
