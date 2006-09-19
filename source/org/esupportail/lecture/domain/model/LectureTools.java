package org.esupportail.lecture.domain.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.esupportail.lecture.dao.DaoService;
import org.esupportail.lecture.domain.service.DomainService;

//import java.util.ArrayList;
//import java.util.Hashtable;
//import java.util.List;

/**
 * Class where are defined differents user attributes, provided by portlet container,
 * used by the lecture channel.
 * @author gbouteil
 *
 */
/**
 * @author gbouteil
 *
 */
public class LectureTools {
	
	/*
	 ************************** PROPERTIES *********************************/	

	/**
	 * Log instance
	 */
	protected static final Log log = LogFactory.getLog(LectureTools.class);

	
	/**
	 * Attribute name used to identified the user profile.
	 * It is defined in the channel config
	 */
	public static String USER_ID;
	
	/**
	 * Name of the portlet preference that set a context to an instance of the channel
	 */
	public static final String CONTEXT = "context";
	
	/**
	 * Current DaoService initialised duriant portlet init
	 */
	private static DaoService daoService;
	
	// voir leur 
	//lors de l'utilisation de ces attributs, verifier leur existance dans cette liste
//	/**
//	 * List of attributes used 
//	 */
//	private static List<String> ATTRIBUTES;

	/*
	 ************************** Initialization ************************************/

//	public static void init(){
//		ATTRIBUTES = new ArrayList();
//	}

	/*
	 *************************** METHODS ************************************/
	
	/* ************************** ACCESSORS ********************************* */

	/**
	 * @param userId
	 * @see LectureTools#USER_ID
	 */
	public static void setUSER_ID(String userId){
		USER_ID=userId;
	}
	
	
	/**
	 * @return USER_ID
	 * @see LectureTools#USER_ID
	 */
	public static String getUSER_ID(){
		return USER_ID;
	}

	/**
	 * Return an instance of current DaoService initialised by Spring
	 * @return current DomainService
	 */
	public static DaoService getDaoService() {
		return LectureTools.daoService;
	}

	/**
	 * set current DaoService (used by Spring)
	 */
	public static void setDaoService(DaoService daoService) {
		LectureTools.daoService = daoService;
	}
	
//	/**
//	 * @return Returns the aTTRIBUTES.
//	 */
//	public static List<String> getATTRIBUTES() {
//		return ATTRIBUTES;
//	}
	
//	/**
//	 * @param attributes The aTTRIBUTES to set.
//	 */
//	public static void setATTRIBUTES(List<String> attributes) {
//		ATTRIBUTES = attributes;
//	}
//
//	public static void setAttribute(String attributeName){
//		ATTRIBUTES.add(attributeName);
//	}
	

	
}
