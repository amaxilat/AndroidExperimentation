package eu.smartsantander.androidExperimentation;

/**
 * Created with IntelliJ IDEA.
 * User: theodori
 * Date: 9/4/13
 * Time: 11:17 AM
 * To change this template use File | Settings | File Templates.
 */
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.net.URL;


public class HibernateUtil {
    private static SessionFactory sessionFactory;
    private static final ThreadLocal session = new ThreadLocal();
    private static final Log log = LogFactory.getLog(HibernateUtil.class);

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            Configuration configuration = null;
            URL configFileURL = null;
            try {
                configFileURL = HibernateUtil.class.getResource("/hibernate.cfg.xml");
                configuration = new Configuration().configure(configFileURL);
                sessionFactory = configuration.buildSessionFactory();
                log.info("Hibernate Initialized Successfully===========================");
            } catch (HibernateException e) {
                log.error("Error initializing Hibernate: " + e.getMessage());
            }
        }
        return sessionFactory;
    }

    public static Session currentSession() throws HibernateException {
        Session s = (Session) session.get();
        if ((s == null)) {
            s = getSessionFactory().openSession();
            session.set(s);
        }
        return s;
    }

    public static void closeSession() throws HibernateException {
        Session s = (Session) session.get();
        if (s != null) {
            s.close();
        }
        session.set(null);
    }

}