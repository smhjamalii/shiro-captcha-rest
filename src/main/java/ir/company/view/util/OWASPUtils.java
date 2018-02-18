package ir.company.view.util;

import ir.company.view.config.ApplicationConfiguration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import redis.clients.jedis.Jedis;

/**
 *
 * @author Mohammad-Hossein Jamali
 */
public class OWASPUtils {    
    
    /**
     * Stops current session and creates new one to prevent Session Fixation.
     * @param subject
     * @param token 
     */
    public static void login(Subject subject, UsernamePasswordToken token){
        Session session = subject.getSession();

        //retain Session attributes to put in the new session after login:
        Map attributes = new LinkedHashMap();
        Collection<Object> keys = session.getAttributeKeys();
        for (Object key : keys) {
            Object value = session.getAttribute(key);
            if (value != null) {
                attributes.put(key, value);
            }
        }

        session.stop();

        //this will create a new session by default in applications that allow session state:
        subject.login(token);

        //restore the attributes:
        session = subject.getSession();
        for (Object key : attributes.keySet()) {
            session.setAttribute(key, attributes.get(key));
        }
        session.setAttribute("uid", token.getUsername());
        
        Jedis jedis = new Jedis(ApplicationConfiguration.REDIS_SERVER_ADDRESS);
        jedis.lpush(token.getUsername(), session.getId().toString());
    }
    
}
