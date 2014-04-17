/* (C) 2014 iMath Research S.L. - All rights reserved.  */

package com.imath.core.rest;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Encoded;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;

import com.imath.core.model.File;
import com.imath.core.model.Job;
import com.imath.core.model.Session;
import com.imath.core.model.Job.States;
import com.imath.core.model.JobResult;
import com.imath.core.model.IMR_User;
import com.imath.core.model.MathGroup;
import com.imath.core.model.MathFunction;
import com.imath.core.data.MainServiceDB;
//import com.imath.core.rest.pub.Exec.Param;
//import com.imath.core.rest.PluginService.ParamDTO;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLEncoder;
//import java.io.IOException;
//import java.io.IOException;
//import java.util.HashMap;
import java.util.HashSet;
//import java.util.HashMap;
//import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import com.imath.core.model.Job.States;

//Just to check
import com.imath.core.service.JobController;
import com.imath.core.service.JobController.Pair;
import com.imath.core.service.JobPythonController;
import com.imath.core.util.Constants;

import java.util.logging.Logger;

/**
* A REST web service that notifies output results from previously issued web services to python nodes 
* 
* @author ammartinez
*/
@Path("/jobpython_service")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@Stateful
public class JobPythonService {
	
	@Inject private MainServiceDB db;
	@Inject private Logger LOG;
	
	@Inject private JobController jc;
	@Inject private JobPythonController jpc;
	
	@GET
    @Path("/python/exec/{idJob}/{result}")//{result}
    @Produces(MediaType.APPLICATION_JSON)
    public void REST_placeOutputPythonJob(@PathParam("idJob") Long idJob,  @PathParam("result") String result) {		
		try {
			//LOG.info("EN LA LLAMADA RES");
			//LOG.info(result);
			States state = States.FINISHED_OK;
			/*if (result==null) { // TODO: We should establish an error codification, and messaging...
				state = States.FINISHED_ERROR;
			}*/
			
			//jc.reportJobPythonFinalization(idJob, state);
			jc.reportJobFinalization(idJob, state, result);
			
		}
		catch (Exception e) {
			LOG.severe("Error placing WS result. idJob: " + idJob);
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
    }
	
	
	@POST
    @Path("/submitJob/{userName}/{idFile}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    public void REST_submitJob(@PathParam("userName") String userName, @PathParam("idFile") Long idFile) {		
		
		Set<File> files = new HashSet<File>(); 
		
		try {
			
			Session session = getSession(userName);		
			File file = getFile(idFile);			
			files.add(file);
			
			List<Param> params = new ArrayList<Param>();
			Param p1 = new Param();
	        p1.setKey(Constants.HPC2_REST_SUBMITJOB_KEY_FILENAME);
	        p1.setValue(file.getUrl());
	        Param p2 = new Param();
	        p2.setKey(Constants.HPC2_REST_SUBMITJOB_KEY_DIRECTORY);
	        p2.setValue(file.getDir().getUrl());
	        
	        params.add(p1);
            params.add(p2);
			
			String paramsString = generateExtraParams(params);
            //System.out.println("Extra params " + paramsString);
          
            Pair pair = jpc.callPythonExec(session, paramsString, files);
           
            jc.makeAJAXCall(pair);
		}
		catch (Exception e) {
			LOG.severe("Error submitting job for userName: " + userName + " - " + e.getMessage());
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
    }
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	private Session getSession(String userName) {
		return db.getSessionDB().findByUser_and_OpenSession(userName);
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	private File getFile(Long idFile) {
		return db.getFileDB().findById(idFile);
	}
	
	private static class Param {
        private String key;
        private String value;
        
        public Param() {}
        public Param(String key, String value) {
            this.key = key;
            this.value = value;
        }
        
        public void setKey(String key) {
            this.key = key;
        }
        
        public String getKey() {
            return this.key;
        }
        
        public void setValue(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return this.value;
        }
    }
	
	 private String generateExtraParams(List<Param> params) throws Exception {
	        String out = "";
	        for(Param param:params) {
	            String assign = param.getKey() + "=" + URLEncoder.encode(param.getValue(), "UTF-8");
	            if (!out.equals("")) {
	                // We are not the first in the list
	                out = out + "&";
	            }
	            out = out + assign;
	        }
	        return out;
	    }
	
}