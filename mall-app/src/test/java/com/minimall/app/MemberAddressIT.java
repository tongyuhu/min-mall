package com.minimall.app;
import com.minimall.app.support.IntegrationTestSupport;
import com.minimall.user.account.application.MemberService;
import com.minimall.user.address.application.*;
import com.minimall.common.api.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class MemberAddressIT extends IntegrationTestSupport {
    @Autowired MemberService members;
    @Autowired AddressService addresses;
    String name(){return "u"+UUID.randomUUID().toString().replace("-","").substring(0,20);}
    @Test void preservesSnapshotsAndEnforcesOwnership() {
        long first=members.register(name(),"learning-password");
        long second=members.register(name(),"learning-password");
        long id=addresses.save(first,null,new AddressInput("张三","13800000000","浙江","杭州","西湖","原地址",true));
        var snapshot=addresses.snapshot(first,id);
        addresses.save(first,id,new AddressInput("张三","13800000000","浙江","杭州","西湖","新地址",true));
        assertEquals("原地址",snapshot.detail());
        assertThrows(BusinessException.class,()->addresses.snapshot(second,id));
        addresses.save(first,null,new AddressInput("李四","13800000001","浙江","杭州","西湖","另一地址",true));
        assertEquals(1,addresses.list(first).stream().filter(a->a.address().defaultAddress()).count());
    }
    @Test void rejectsDuplicateUsernameAndInvalidatesCredentialVersion() {
        String username=name(); long id=members.register(username,"learning-password");
        assertThrows(RuntimeException.class,()->members.register(username,"learning-password"));
        members.changePassword(id,"learning-password","another-password");
        assertThrows(BusinessException.class,()->members.changePassword(id,"learning-password","third-password"));
    }
}
