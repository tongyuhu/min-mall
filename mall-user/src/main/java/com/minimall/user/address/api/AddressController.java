package com.minimall.user.address.api;
import com.minimall.user.address.application.*;import com.minimall.common.auth.*;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestController @RequestMapping("/api/v1/addresses") public class AddressController {
 private final AddressService service;private final CurrentActor actor;public AddressController(AddressService service,CurrentActor actor){this.service=service;this.actor=actor;}
 @GetMapping public List<AddressView> list(){return service.list(actor.require(ActorType.MEMBER));}
 @PostMapping public Map<String,String> create(@RequestBody AddressInput input){return Map.of("id",Long.toString(service.save(actor.require(ActorType.MEMBER),null,input)));}
 @PutMapping("/{id}") public void update(@PathVariable long id,@RequestBody AddressInput input){service.save(actor.require(ActorType.MEMBER),id,input);}
 @DeleteMapping("/{id}") public void delete(@PathVariable long id){service.delete(actor.require(ActorType.MEMBER),id);}
}
