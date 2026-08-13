package ru.otus.hw.service;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.otus.hw.model.Author;

@DisplayName("ACL сервис")
@ExtendWith(MockitoExtension.class)
class AclServiceWrapperServiceImplTest {

  @Mock
  private MutableAclService mutableAclService;

  private AclServiceWrapperServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new AclServiceWrapperServiceImpl(mutableAclService);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
            "alice",
            "N/A",
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        )
    );
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("создает ACL владельца для текущего пользователя")
  void shouldCreateAclOwnedByCurrentPrincipal() {
    Author author = new Author(42L, "Author");
    MutableAcl acl = mock(MutableAcl.class);
    when(acl.getEntries()).thenReturn(List.of());
    when(mutableAclService.createAcl(new ObjectIdentityImpl(author))).thenReturn(acl);

    service.createPermission(author, BasePermission.READ);

    verify(acl).setOwner(new PrincipalSid("alice"));
    verify(mutableAclService).updateAcl(acl);
  }

  @Test
  @DisplayName("выдает все запрашиваемые права для владельца и ROLE_ADMIN")
  void shouldGrantRequestedPermissionsToOwnerAndAdmin() {
    Author author = new Author(42L, "Author");
    MutableAcl acl = mock(MutableAcl.class);
    when(acl.getEntries()).thenReturn(List.of());
    when(mutableAclService.createAcl(new ObjectIdentityImpl(author))).thenReturn(acl);

    service.createPermission(
        author,
        BasePermission.READ,
        BasePermission.WRITE,
        BasePermission.DELETE
    );

    PrincipalSid owner = new PrincipalSid("alice");
    GrantedAuthoritySid admin = new GrantedAuthoritySid("ROLE_ADMIN");

    verify(acl).insertAce(anyInt(), eq(BasePermission.READ), eq(owner), eq(true));
    verify(acl).insertAce(anyInt(), eq(BasePermission.WRITE), eq(owner), eq(true));
    verify(acl).insertAce(anyInt(), eq(BasePermission.DELETE), eq(owner), eq(true));

    verify(acl).insertAce(anyInt(), eq(BasePermission.READ), eq(admin), eq(true));
    verify(acl).insertAce(anyInt(), eq(BasePermission.WRITE), eq(admin), eq(true));
    verify(acl).insertAce(anyInt(), eq(BasePermission.DELETE), eq(admin), eq(true));
  }

  @Test
  @DisplayName("всегда выдает ACL на администрирование для ROLE_ADMIN")
  void shouldGrantAdministrationToAdmin() {
    Author author = new Author(42L, "Author");
    MutableAcl acl = mock(MutableAcl.class);
    when(acl.getEntries()).thenReturn(List.of());
    when(mutableAclService.createAcl(new ObjectIdentityImpl(author))).thenReturn(acl);

    service.createPermission(author, BasePermission.READ);

    verify(acl).insertAce(
        anyInt(),
        eq(BasePermission.ADMINISTRATION),
        eq(new GrantedAuthoritySid("ROLE_ADMIN")),
        eq(true)
    );
  }

  @Test
  @DisplayName("удаляет ACL по id сущности и классу")
  void shouldDeleteAclByEntityIdAndType() {
    service.deletePermissions(42L, ru.otus.hw.model.Book.class);

    verify(mutableAclService).deleteAcl(
        new ObjectIdentityImpl(ru.otus.hw.model.Book.class, 42L),
        true
    );
  }
}
