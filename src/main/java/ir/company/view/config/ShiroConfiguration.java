package ir.company.view.config;

import javax.enterprise.inject.Produces;
import javax.sql.DataSource;
import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.authc.credential.PasswordMatcher;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.DefaultHashService;
import org.apache.shiro.crypto.hash.Sha512Hash;
import org.apache.shiro.jndi.JndiObjectFactory;
import org.apache.shiro.mgt.RememberMeManager;
import org.apache.shiro.realm.jdbc.JdbcRealm;
import org.apache.shiro.session.mgt.ExecutorServiceSessionValidationScheduler;
import org.apache.shiro.web.filter.authc.AnonymousFilter;
import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;
import org.apache.shiro.web.filter.authc.UserFilter;
import org.apache.shiro.web.filter.mgt.DefaultFilterChainManager;
import org.apache.shiro.web.filter.mgt.FilterChainManager;
import org.apache.shiro.web.filter.mgt.FilterChainResolver;
import org.apache.shiro.web.filter.mgt.PathMatchingFilterChainResolver;
import org.apache.shiro.web.mgt.CookieRememberMeManager;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.apache.shiro.web.servlet.ShiroFilter;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.crazycake.shiro.RedisCacheManager;
import org.crazycake.shiro.RedisManager;
import org.crazycake.shiro.RedisSessionDAO;

/**
 *
 * @author Mohammad-Hossein Jamali
 */
public class ShiroConfiguration {
    
    private FilterChainResolver filterChainResolver = null;
    private DefaultWebSecurityManager securityManager = null;
    private DefaultWebSessionManager sessionManager = null;
    private RedisManager redisManager = null;
    private RedisCacheManager redisCacheManager = null;
    private ExecutorServiceSessionValidationScheduler sessionValidatorScheduler = null;
    private DefaultPasswordService passwordService = null;
    private PasswordMatcher passwordMatcher = null;
    
    private ShiroConfiguration() {
    }

    @Produces    
    public ExecutorServiceSessionValidationScheduler getExecutorServiceSessionValidationScheduler(){
        if(sessionValidatorScheduler == null) {
            sessionValidatorScheduler = new ExecutorServiceSessionValidationScheduler();
            sessionValidatorScheduler.setInterval(3600000);            
        }
        return sessionValidatorScheduler;
    }
    
    @Produces
    public RedisCacheManager getRedisCacheManager(){
        if(redisCacheManager == null){
            redisCacheManager = new RedisCacheManager();
            redisCacheManager.setKeyPrefix("shiro:cache:");
            redisCacheManager.setRedisManager(getRedisManager());            
        }        
        return redisCacheManager;
    }
    
    @Produces
    public RedisManager getRedisManager(){
        if(redisManager == null) {
            redisManager = new RedisManager();
            redisManager.setHost("127.0.0.1");
            redisManager.setPort(6379);
            redisManager.setExpire(600);
            redisManager.setTimeout(0);
            
            RedisSessionDAO redisSessionDao = new RedisSessionDAO();
            redisSessionDao.setKeyPrefix("shiro:session:");
            redisSessionDao.setRedisManager(redisManager);
            getSessionManager().setSessionDAO(redisSessionDao);
            
        }
        return redisManager;
    }
    
    @Produces
    public DefaultWebSessionManager getSessionManager(){
        if(sessionManager == null) {
            sessionManager = new DefaultWebSessionManager();
            sessionManager.setGlobalSessionTimeout(1800000);                                    
            sessionManager.setSessionValidationScheduler(getExecutorServiceSessionValidationScheduler());
            sessionManager.setSessionValidationSchedulerEnabled(Boolean.TRUE);
            sessionManager.setDeleteInvalidSessions(Boolean.TRUE);
        }
        return sessionManager;
    }
    
    @Produces
    public DefaultPasswordService getPasswordService(){
        if(passwordService == null){
            DefaultHashService hashService = new DefaultHashService();
            hashService.setHashIterations(500000);
            hashService.setHashAlgorithmName("SHA-512");
            hashService.setGeneratePublicSalt(Boolean.TRUE);
            Sha512Hash sha512 = new Sha512Hash();
            sha512.setIterations(500000);
            sha512.setSalt(new SecureRandomNumberGenerator().nextBytes());
            hashService.setPrivateSalt(sha512);
            passwordService = new DefaultPasswordService();
            passwordService.setHashService(hashService);        
        }
        return passwordService;
    }
    
    @Produces
    public PasswordMatcher getPasswordMatcher(){
        if(passwordMatcher == null){
            passwordMatcher = new PasswordMatcher();
            passwordMatcher.setPasswordService(getPasswordService());            
        }
        return passwordMatcher;
    }
    
    @Produces
    public WebSecurityManager getSecurityManager() {
        if(securityManager == null) {           
            JndiObjectFactory jndiObjectFactory = new JndiObjectFactory();
            jndiObjectFactory.setRequiredType(javax.sql.DataSource.class);
            jndiObjectFactory.setResourceName("java:/jdbc/orderhandler");        
            JdbcRealm realm = new JdbcRealm();
            realm.setPermissionsLookupEnabled(Boolean.FALSE);
            realm.setDataSource((DataSource) jndiObjectFactory.getInstance());
            realm.setAuthenticationQuery("SELECT password FROM tbl_user WHERE username = ?");
            realm.setCredentialsMatcher(passwordMatcher);
            securityManager = new DefaultWebSecurityManager(realm);
            RememberMeManager rememberMeManager = new CookieRememberMeManager();
            ((CookieRememberMeManager) rememberMeManager).setCipherKey("kPH+bIxk5D2deZiIxcaaaA==".getBytes());
            securityManager.setRememberMeManager(rememberMeManager);
            securityManager.setSessionManager(getSessionManager());
            securityManager.setCacheManager(getRedisCacheManager());            
        }
        return securityManager;
    }

    @Produces
    public FilterChainResolver getFilterChainResolver() {        
        if (filterChainResolver == null) {
            
            FormAuthenticationFilter authc = new FormAuthenticationFilter();
            AnonymousFilter anon = new AnonymousFilter();
            UserFilter user = new UserFilter();
            ShiroFilter shiro = new ShiroFilter();
            
            authc.setLoginUrl(WebPages.LOGIN_URL);
            //authc.setSuccessUrl(successUrl);            
            user.setLoginUrl(WebPages.LOGIN_URL);
            FilterChainManager fcMan = new DefaultFilterChainManager();
            
            fcMan.addFilter("authc", authc);
            fcMan.addFilter("anon", anon);
            fcMan.addFilter("user", user);
            fcMan.addFilter("shiro", shiro);
            
            fcMan.createChain("/faces/guest/**", "anon");
            fcMan.createChain("/css/**", "anon");
            fcMan.createChain("/js/**", "anon");
            
            fcMan.createChain("/faces/user/**", "authc");                        
                                    
            PathMatchingFilterChainResolver resolver = new PathMatchingFilterChainResolver();
            resolver.setFilterChainManager(fcMan);
            filterChainResolver = resolver;
        }
        return filterChainResolver;        
    }    
    
}