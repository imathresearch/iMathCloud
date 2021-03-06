/* (C) 2013 iMath Research S.L. - All rights reserved.  */

package com.imath.core.rest;

import com.imath.core.security.SecurityManager;
import com.imath.core.service.PluginController.PercDTO;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import com.imath.core.model.Job;
import com.imath.core.model.File;
import com.imath.core.model.Job.States;
import com.imath.core.model.JobResult;
import com.imath.core.data.MainServiceDB;
import com.imath.core.service.FileController;
import com.imath.core.service.JobController;
import com.imath.core.service.PluginController;
import com.imath.core.util.Constants;
import com.imath.core.util.PublicResponse;
import com.imath.core.rest.FileService.FileDTO;

import java.util.logging.Logger;

/**
 * A REST web service that provides access to file controller
 * 
 * @author ipinyol
 */
@Path("/job_service")
@RequestScoped
@Stateful
public class JobService {
	@Inject private FileController fc;
	@Inject private MainServiceDB db;
	@Inject private Logger LOG;
	@Inject private JobController jc; 
	
	@Inject private PluginController pc;
	
	private static String LOG_PRE = Constants.LOG_PREFIX_SYSTEM + "[JobService]";
	
	@GET
    @Path("/getJobsRunning/{userName}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<JobDTO> REST_getJobs(@PathParam("userName") String userName, @Context SecurityContext sc) {
	    LOG.info(LOG_PRE + "[getJobsRunning]" +userName);
		try {
		    SecurityManager.secureBasic(userName, sc);
			List<Job> jobs = db.getJobDB().getJobsByUser_and_State(userName, Job.States.RUNNING);
			return prepareJobsToSubmit(jobs);
		}
		catch (Exception e) {
			LOG.severe("Error getting the jobs from user: "+ userName + " - " + e.getMessage());
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
    }
	
	@GET
    @Path("/getJobs/{userName}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<JobDTO> REST_getJobsAll(@PathParam("userName") String userName, @Context SecurityContext sc) {
		//TODO: Test needed!!
		//TODO: Authenticate the call. Make sure that it is done from index.html
		// and that the user is authenticated
		//System.out.println("GETTING JOBS");
        LOG.info(LOG_PRE + "[getJobs]" +userName);
		try {
		    SecurityManager.secureBasic(userName, sc);
			List<Job> jobs =  db.getJobDB().getJobsByUser(userName);
			return prepareJobsToSubmit(jobs);
		}
		catch (Exception e) {
			LOG.severe("Error getting the jobs from user: "+ userName + " - " + e.getMessage());
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
    }
	
	@GET
    @Path("/getJob/{idJob}")
    @Produces(MediaType.APPLICATION_JSON)
    public JobDTO REST_getJob(@PathParam("idJob") Long idJob, @Context SecurityContext sc) {
	    LOG.info(LOG_PRE + "[getJobs]" + idJob.toString());
		try {
		    Job job = db.getJobDB().findByIdSecured(idJob, sc.getUserPrincipal().getName());
			//Job job =  db.getJobDB().findById(idJob);
			return prepareJobToSubmit(job);
		}
		catch (Exception e) {
			LOG.severe("Error getting job id: "+ idJob + " - " + e.getMessage());
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
    }
	
	@GET
    @Path("/getJobOutputFiles/{idJob}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FileDTO> REST_getJobOutputFiles(@PathParam("idJob") Long idJob, @Context SecurityContext sc) {
	    LOG.info(LOG_PRE + "[getJobOutputFiles]" + idJob.toString());
		try {
		    Job job = db.getJobDB().findByIdSecured(idJob, sc.getUserPrincipal().getName());
			//Job job =  db.getJobDB().findById(idJob);
			return PrepareToSubmitFiles(job.getOutputFiles());
		}
		catch (Exception e) {
			LOG.severe("Error getting job id: "+ idJob + " - " + e.getMessage());
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
    }
	
	
	@DELETE
    @Path("/removeJob/{idJob}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response REST_removeJob(@PathParam("idJob") Long idJob, @Context SecurityContext sc) {
	    LOG.info(LOG_PRE + "[removeJob]" + idJob.toString());
        try{
            Job job = jc.getJobStructure(idJob, sc);
            if (job == null) {
           	 return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            } else {
                jc.removeJob(idJob);
                return Response.status(Response.Status.OK).build();
            }
        }
        catch (Exception e) {
        	LOG.severe("Error getting job id: "+ idJob + " - " + e.getMessage());
        	return Response.status(Response.Status.NOT_FOUND).build();
        }		
    }
	
	private List<JobDTO> prepareJobsToSubmit(List<Job> jobs) throws Exception {
		List<JobDTO> jobsdto = new ArrayList<JobDTO>();
		Iterator<Job> it = jobs.iterator();
		while(it.hasNext()) {
			Job job = it.next();
			jobsdto.add(prepareJobToSubmit(job));
		}
		return jobsdto;
	}
	
	private List<FileDTO> PrepareToSubmitFiles(Set<File> files) {
	    return PrepareToSubmitFilesStatic(files);
	}
	
	private static List<FileDTO> PrepareToSubmitFilesStatic(Set<File> files) {
        List<FileDTO> ret = new ArrayList<FileDTO>();
        if (files==null) return ret;
        Iterator<File> it = files.iterator();
        while(it.hasNext()) {
            File file = it.next();
            FileDTO fileDTO = new FileDTO();
            fileDTO.id = file.getId();
            fileDTO.name=file.getName();
            fileDTO.type=file.getIMR_Type();
            fileDTO.dir = file.getDir().getId();
            fileDTO.sharingState = file.getSharingState();
            
            fileDTO.userNameOwner = file.getOwner().getUserName();
            
            ret.add(fileDTO);
        }
        return ret;
    }
	private JobDTO prepareJobToSubmit(Job job) throws Exception {
		JobDTO jobdto = new JobDTO();
		jobdto.id = job.getId();
		jobdto.endDate=job.getEndDate();
		jobdto.startDate=job.getStartDate();
		jobdto.description=job.getDescription();
		jobdto.state=job.getState();
		jobdto.jobResult=job.getJobResult();		
		
		//To manage the percentages
		jobdto.pcts = pc.getCompletionPercentages(jobdto.id);
		if(jobdto.pcts.getPerc().isEmpty()){
			jobdto.pcts.getPerc().add("NO INFO");
		}
		jobdto.outputFiles = PrepareToSubmitFilesStatic(job.getOutputFiles()); 
		return jobdto;
	}
	
	public static class JobDTO {
		public Long id;
		public Date startDate;
		public Date endDate;
		public States state;
		public String description;
		public JobResult jobResult;		
		public PercDTO pcts;
		public List<FileDTO> outputFiles;
		
		public JobDTO(){}
		public void jobToJobDTO(Job job) {
		    this.id = job.getId();
		    this.endDate=job.getEndDate();
		    this.startDate=job.getStartDate();
		    this.description=job.getDescription();
		    this.state=job.getState();
		    this.jobResult=job.getJobResult();
		    this.pcts = new PercDTO();
		    this.pcts.setPerc(new ArrayList<String>());
		    this.pcts.getPerc().add("NO INFO");
		    this.outputFiles = PrepareToSubmitFilesStatic(job.getOutputFiles()); 
		}
	}
	
	

}
