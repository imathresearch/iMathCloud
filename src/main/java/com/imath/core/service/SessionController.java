/* (C) 2013 iMath Research S.L. - All rights reserved.  */

package com.imath.core.service;

import java.util.Date;
import java.util.List;
//import java.util.ArrayList;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.imath.core.model.Session;
import com.imath.core.model.IMR_User;
import com.imath.core.model.Host;

import com.imath.core.data.MainServiceDB;

import java.util.logging.Logger;

/**
 * The Sessions Controller class. It offers a set of methods to control sessions in Core of  
 * iMath Services. Each user (both web-based or through the Rest interface) that is authenticated
 * in the system and is using our services will have an instance of {@link Session}, 
 * which will be controlled in this class
 * 
 * @author ipinyol
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class SessionController extends AbstractController{
    
    /**
     * It closes down the open session of the user. 
     * @param String - The authenticated user name of the system. It must be a valid {@link User} id.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void closeSession(String userName) throws Exception {
    	// TODO: NOT Tested
    	LOG.info("User:" + userName + " requesting closing the session");
    	try {
    		Session session = db.getSessionDB().findByUser_and_OpenSession(userName);
    		if (session==null) {
    			LOG.warning("No open session for user: " + userName);
    		}
    		else {
    			session.setEndDate(new Date());
    			//TODO: Manage to close the connection with Tornado 
    			try {
        			db.beginTransaction();
        			db.makePersistent(session);
        			db.commitTransaction();
        		}
        		catch (Exception e) {
        			LOG.severe("Error saving session: " + e.getMessage());
        			db.rollBackTransaction();
        			throw e;
        		}	
    		}
    	}
    	catch (Exception e) {
    		LOG.severe("Error retreiving sessions");
    		throw e;
    	}
    }
    
    /**
     * It requests a new web session. It it does not exists, the method creates the a Session and returns it. 
     * Otherwise, it returns the open stored one.  
     * @param String - The authenticated user name of the system. It must be a valid {@link User} id.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Session requestWebSession(String userName) throws Exception {
    	LOG.info("User:" + userName + " requesting a session");
    	try {
    		Session session = db.getSessionDB().findByUser_and_OpenSession(userName);
    		if (session==null) {
        		// In this case we need to create a new iMath Session
        		session = new Session();
        		
        		// We get the user
        		IMR_User user = db.getIMR_UserDB().findById(userName);
        		if (user==null) {
        			LOG.severe("User " + userName + " not found");
        			throw new Exception();
        		}
        		session.setUser(user);
        		session.setStartDate(new Date());
        		
        		// We get the list of potential hosts for the interactive math console
        		List<Host> hosts= db.getHostDB().getHostByConsole_and_State(true, true);
        		if (hosts.size()==0) {
        			LOG.severe("No hosts available for interactive console");
        			throw new Exception();
        		}
        		// TODO: Select the best host for interactive math console. 
        		// Now, we get the first if the list
        		session.setHostConsole(hosts.get(0));
        		session.setEndDate(null);
        		try {
        			db.beginTransaction();
        			db.makePersistent(session);
        			db.commitTransaction();
        			
        		}
        		catch (Exception e) {
        			LOG.severe("Error saving session: " + e.getMessage());
        			db.rollBackTransaction();
        			throw e;
        		}	
        	}
    		//List<String> params = new ArrayList<String>();
    		//params.add("%2Fhome%2Fipinyol%2Ftest.csv");
    		//pc.callPlugin(new Long(1), session, params); // Just to test. PROVISIONAL
        	return session;
    	}
    	catch (Exception e) {
    		LOG.severe("Error retreiving sessions");
    		throw e;
    	}
    	
    }
    
    //public Session sessionRequestRest(String publicKey) throws Exception {
    //	LOG.info("User with public key :" + publicKey + " requesting a session");
    //}
    
}