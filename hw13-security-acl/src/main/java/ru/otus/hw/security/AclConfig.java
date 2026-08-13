package ru.otus.hw.security;

import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.acls.AclPermissionCacheOptimizer;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.acls.domain.AclAuthorizationStrategy;
import org.springframework.security.acls.domain.AclAuthorizationStrategyImpl;
import org.springframework.security.acls.domain.ConsoleAuditLogger;
import org.springframework.security.acls.domain.DefaultPermissionGrantingStrategy;
import org.springframework.security.acls.domain.SpringCacheBasedAclCache;
import org.springframework.security.acls.jdbc.BasicLookupStrategy;
import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.PermissionGrantingStrategy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Configuration
@EnableMethodSecurity(securedEnabled = true)
@RequiredArgsConstructor
public class AclConfig {

  private final DataSource dataSource;

  @Bean
  public AclCache aclCache() {
    final ConcurrentMapCache aclCache = new ConcurrentMapCache("acl_cache");
    return new SpringCacheBasedAclCache(aclCache, permissionGrantingStrategy(), aclAuthorizationStrategy());
  }

  @Bean
  public PermissionGrantingStrategy permissionGrantingStrategy() {
    return new DefaultPermissionGrantingStrategy(new ConsoleAuditLogger());
  }

  @Bean
  public AclAuthorizationStrategy aclAuthorizationStrategy() {
    return new AclAuthorizationStrategyImpl(new SimpleGrantedAuthority("ROLE_ADMIN"));
  }

  @Bean
  public LookupStrategy lookupStrategy() {
    return new BasicLookupStrategy(dataSource, aclCache(), aclAuthorizationStrategy(), new ConsoleAuditLogger());
  }

  @Bean
  public MutableAclService aclService() {
    JdbcMutableAclService service = new JdbcMutableAclService(dataSource, lookupStrategy(), aclCache());

    service.setClassIdentityQuery("SELECT ID FROM ACL_CLASS ORDER BY ID DESC LIMIT 1");
    service.setSidIdentityQuery("SELECT ID FROM ACL_SID ORDER BY ID DESC LIMIT 1");

    return service;
  }

  @Bean
  public MethodSecurityExpressionHandler defaultMethodSecurityExpressionHandler() {
    DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
    AclPermissionEvaluator permissionEvaluator = new AclPermissionEvaluator(aclService());
    expressionHandler.setPermissionEvaluator(permissionEvaluator);
    expressionHandler.setPermissionCacheOptimizer(new AclPermissionCacheOptimizer(aclService()));
    return expressionHandler;
  }
}
