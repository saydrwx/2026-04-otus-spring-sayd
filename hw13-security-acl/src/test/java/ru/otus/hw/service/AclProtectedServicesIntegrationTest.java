package ru.otus.hw.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.dto.AuthorForm;
import ru.otus.hw.dto.BookForm;
import ru.otus.hw.dto.CommentForm;
import ru.otus.hw.dto.GenreForm;
import ru.otus.hw.model.Author;
import ru.otus.hw.model.Book;
import ru.otus.hw.model.Comment;
import ru.otus.hw.model.Genre;

@DisplayName("Защищенные ACL сервисные методы")
@SpringBootTest
@Transactional
class AclProtectedServicesIntegrationTest {

  @Autowired
  private AuthorService authorService;

  @Autowired
  private GenreService genreService;

  @Autowired
  private BookService bookService;

  @Autowired
  private CommentService commentService;

  @Autowired
  private MutableAclService mutableAclService;

  @Autowired
  private AclCache aclCache;

  @AfterEach
  void clearAclCache() {
    aclCache.clearCache();
  }

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  @DisplayName("ROLE_ADMIN может изменять любые сущности с WRITE правами")
  void shouldAllowAdminToUpdateAllEntityTypesWithWritePermission() {
    Sid admin = new GrantedAuthoritySid("ROLE_ADMIN");
    grant(Author.class, 1L, admin, BasePermission.WRITE);
    grant(Genre.class, 1L, admin, BasePermission.WRITE);
    grant(Book.class, 1L, admin, BasePermission.WRITE);
    grant(Comment.class, 1L, admin, BasePermission.WRITE);

    assertDoesNotThrow(() -> authorService.update(1L, new AuthorForm("Updated Author")));
    assertDoesNotThrow(() -> genreService.update(1L, new GenreForm("Updated Genre")));
    assertDoesNotThrow(() -> bookService.update(
        1L,
        new BookForm("Updated Book", 1L, Set.of(1L, 2L))
    ));
    assertDoesNotThrow(() -> commentService.update(1L, new CommentForm("Updated comment")));
  }

  @Test
  @WithMockUser(username = "user", roles = "USER")
  @DisplayName("READ само по себе не дает возможность изменения сущности")
  void shouldDenyUpdatesWhenOnlyReadPermissionExists() {
    Sid user = new GrantedAuthoritySid("ROLE_USER");
    grant(Author.class, 1L, user, BasePermission.READ);
    grant(Genre.class, 1L, user, BasePermission.READ);
    grant(Book.class, 1L, user, BasePermission.READ);
    grant(Comment.class, 1L, user, BasePermission.READ);

    assertThatThrownBy(() -> authorService.update(1L, new AuthorForm("Updated Author")))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> genreService.update(1L, new GenreForm("Updated Genre")))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> bookService.update(
        1L,
        new BookForm("Updated Book", 1L, Set.of(1L, 2L))
    )).isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> commentService.update(1L, new CommentForm("Updated comment")))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  @DisplayName("ROLE_ADMIN может удалять любые сущности с DELETE правами")
  void shouldAllowAdminToDeleteAllEntityTypesWithDeletePermission() {
    Sid admin = new GrantedAuthoritySid("ROLE_ADMIN");
    grant(Comment.class, 3L, admin, BasePermission.DELETE);
    grant(Book.class, 2L, admin, BasePermission.DELETE);
    grant(Genre.class, 3L, admin, BasePermission.DELETE);
    grant(Author.class, 1L, admin, BasePermission.DELETE);

    assertDoesNotThrow(() -> commentService.deleteById(3L));
    assertDoesNotThrow(() -> bookService.deleteById(2L));
    assertDoesNotThrow(() -> genreService.deleteById(3L));
    assertDoesNotThrow(() -> authorService.deleteById(1L));
  }

  @Test
  @WithMockUser(username = "user", roles = "USER")
  @DisplayName("READ само по себе не дает возможность удаления сущности")
  void shouldDenyDeletesWhenOnlyReadPermissionExists() {
    Sid user = new GrantedAuthoritySid("ROLE_USER");
    grant(Author.class, 1L, user, BasePermission.READ);
    grant(Genre.class, 1L, user, BasePermission.READ);
    grant(Book.class, 1L, user, BasePermission.READ);
    grant(Comment.class, 1L, user, BasePermission.READ);

    assertThatThrownBy(() -> authorService.deleteById(1L))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> genreService.deleteById(1L))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> bookService.deleteById(1L))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> commentService.deleteById(1L))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @WithMockUser(username = "alice", roles = "USER")
  @DisplayName("WRITE у principal дает возможность изменения сущности")
  void shouldAllowPrincipalWithWritePermissionToUpdate() {
    grant(Author.class, 1L, new PrincipalSid("alice"), BasePermission.WRITE);

    assertDoesNotThrow(() -> authorService.update(1L, new AuthorForm("Alice Author")));
  }

  @Test
  @WithMockUser(username = "bob", roles = "USER")
  @DisplayName("права principal не переходят другому")
  void shouldNotGrantPrincipalPermissionToAnotherUser() {
    grant(Author.class, 1L, new PrincipalSid("alice"), BasePermission.WRITE);

    assertThatThrownBy(() -> authorService.update(1L, new AuthorForm("Bob Author")))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @WithMockUser(username = "guest", roles = "GUEST")
  @DisplayName("роль GUEST не дает доступа к @Secured методам")
  void shouldRejectUnsupportedRoleForSecuredReadMethods() {
    assertThatThrownBy(() -> authorService.findAll())
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> genreService.findAll())
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> bookService.findAll())
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> commentService.findAllByBookId(1L))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @WithMockUser(username = "user", roles = "USER")
  @DisplayName("ROLE_USER может вызывать защищенные create методы")
  void shouldAllowUserRoleToCreateEntities() {
    assertDoesNotThrow(() -> authorService.create(new AuthorForm("New Author")));
    assertDoesNotThrow(() -> genreService.create(new GenreForm("New Genre")));
    assertDoesNotThrow(() -> bookService.create(
        new BookForm("New Book", 1L, Set.of(1L, 2L))
    ));
    assertDoesNotThrow(() -> commentService.create(1L, new CommentForm("New comment")));
  }

  private void grant(
      Class<?> entityType,
      long entityId,
      Sid sid,
      Permission... permissions
  ) {
    Authentication originalAuthentication = SecurityContextHolder.getContext().getAuthentication();

    try {
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(
              "acl-seeder",
              "N/A",
              List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
          )
      );

      MutableAcl acl = mutableAclService.createAcl(
          new ObjectIdentityImpl(entityType, entityId)
      );

      for (Permission permission : permissions) {
        acl.insertAce(acl.getEntries().size(), permission, sid, true);
      }

      mutableAclService.updateAcl(acl);
    } finally {
      SecurityContextHolder.getContext().setAuthentication(originalAuthentication);
    }
  }
}
