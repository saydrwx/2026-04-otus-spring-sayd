package ru.otus.hw.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional
public class AclServiceWrapperServiceImpl implements AclServiceWrapperService {

  private final MutableAclService mutableAclService;

  @Override
  public void createPermission(Object object, Permission... permissions) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Sid owner = new PrincipalSid(authentication);
    Sid admin = new GrantedAuthoritySid("ROLE_ADMIN");

    ObjectIdentity oid = new ObjectIdentityImpl(object);
    MutableAcl acl = mutableAclService.createAcl(oid);

    acl.setOwner(owner);

    for (Permission permission : permissions) {
      acl.insertAce(acl.getEntries().size(), permission, owner, true);
      acl.insertAce(acl.getEntries().size(), permission, admin, true);
    }
    acl.insertAce(acl.getEntries().size(), BasePermission.ADMINISTRATION, admin, true);

    mutableAclService.updateAcl(acl);
  }

  @Override
  public void deletePermissions(Long entityId, Class<?> entityClass) {
    var oid = new ObjectIdentityImpl(entityClass, entityId);
    mutableAclService.deleteAcl(oid, true);
  }
}
