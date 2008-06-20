/**
* ESUP-Portail Lecture - Copyright (c) 2006 ESUP-Portail consortium
* For any information please refer to http://esup-helpdesk.sourceforge.net
* You may obtain a copy of the licence at http://www.esup-portail.org/license/
*/
package org.esupportail.lecture.domain.model;


import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.esupportail.commons.exceptions.ConfigException;
import org.esupportail.lecture.dao.DaoService;
import org.esupportail.lecture.domain.DomainTools;
import org.esupportail.lecture.domain.ExternalService;
import org.esupportail.lecture.exceptions.domain.ChannelConfigException;
import org.esupportail.lecture.exceptions.domain.ContextNotFoundException;
import org.esupportail.lecture.exceptions.domain.FatalException;
import org.esupportail.lecture.exceptions.domain.ManagedCategoryProfileNotFoundException;
import org.esupportail.lecture.exceptions.domain.MappingFileException;
import org.esupportail.lecture.exceptions.domain.PrivateException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * The "lecture" channel : main domain model class
 * It is the owner of contexts and managed elements
 * @author gbouteil
 */
public class Channel implements InitializingBean {

	/*
	 ************************** PROPERTIES *********************************/	
	
	/**
	 * Log instance 
	 */
	protected static final Log log = LogFactory.getLog(Channel.class); 

	/* channel's elements */
	
	/**
     * Hashtable of contexts defined in the channel, indexed by their ids.
     */
	private Hashtable<String,Context> contextsHash;
	
	/**
	 * Hashtable of ManagedCategoryProfiles defined in the chanel, indexed by their Id.
	 */
	private Hashtable<String,ManagedCategoryProfile> managedCategoryProfilesHash;
	
	/**
	 * Hashtable of UserProfiles loaded from database, indexed by their userId.
	 */
	private Hashtable<String,UserProfile> userProfilesHash = new Hashtable<String,UserProfile>();
	
	/* mappings */
	
	/**
	 * List of mappings defined by the mapping file.
	 */
	private List<Mapping> mappingList;
	
	/**
	 * Hash to access mappings by dtd
	 */	
	private Hashtable<String,Mapping> mappingHashByDtd;
	
	/**
	 * Hash to access mappings by xmlns
	 */	
	private Hashtable<String,Mapping> mappingHashByXmlns;
	
	/**
	 * Hash to access mappings by xmlType.
	 */	
	private Hashtable<String,Mapping> mappingHashByXmlType;
	
	/**
	 * Hash to access mappings by root element.
	 */	
	private Hashtable<String,Mapping> mappingHashByRootElement;

	/**
	 * Hash to access mappings by sourceURL.
	 */	
	private Hashtable<String,Mapping> mappingHashBySourceURL;

	/* config and mapping files */
	
	/**
	 * relative file path of the config file
	 */
	private String configFilePath;
	
	/**
	 * configLoaded = true if channel config has ever been loaded in channel
	 */
	private boolean configLoaded = false;
	
	/**
	 * relative file path of the mapping file
	 */
	private String mappingFilePath;
	/**
	 * mappingsLoaded = true if channel config has ever been loaded in channel
	 */
	private boolean mappingsLoaded = false;
	
	/* Some services */
	
	/**
	 * access to dao services
	 */
	private DaoService daoService;
	
	/**
	 * acces to external service
	 */
	private ExternalService externalService;

	
	/*
	 ************************** INIT *********************************/	

//	/**
//	 * default constructor
//	 */
//	public Channel(){
//		userProfilesHash = new Hashtable<String,UserProfile>();
//	}
	
	/**
	 * @throws FatalException
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws FatalException   {
		if (log.isDebugEnabled()){
			log.debug("afterPropertiesSet()");
		}
		Assert.notNull(daoService,"property daoService can not be null");
		Assert.notNull(externalService,"property externalService can not be null");
		Assert.notNull(configFilePath,"property configFilePath cannot be null");
		Assert.notNull(mappingFilePath,"property mappingFilePath cannot be null");
		try {
			startup();
		} catch (PrivateException e) {
			String errorMsg = "Unable to startup channel because of a PrivateException.";
			log.fatal(errorMsg);
			throw new FatalException(errorMsg,e);
		} catch (ContextNotFoundException e) {
			String errorMsg = "Unable to startup channel because of a ContextNotFoundException.";
			log.fatal(errorMsg);
			throw new FatalException(errorMsg,e);
		} catch (ManagedCategoryProfileNotFoundException e) {
			String errorMsg = "Unable to startup channel because of a ManagedCategoryProfileNotFoundException.";
			log.fatal(errorMsg);
			throw new FatalException(errorMsg,e);
		} 
		DomainTools.setChannel(this);
		DomainTools.setDaoService(daoService);
		DomainTools.setExternalService(externalService);
	}
	
	/**
	 * Methods called to start channel (load the config and mapping file 
	 * if needed when files are modified from last loading)
	 * @throws ChannelConfigException 
	 * @throws ContextNotFoundException 
	 * @throws ManagedCategoryProfileNotFoundException 
	 * @throws MappingFileException 
	 */
	synchronized private void startup() 
		throws ManagedCategoryProfileNotFoundException, ContextNotFoundException, ChannelConfigException, MappingFileException {
		if (log.isDebugEnabled()){
			log.debug("startup()");
		}
		loadConfig();
		loadMappingFile();
	}
	
	/**
	 * Load config file if is modified since last loading (using ChannelConfig), 
	 * It gets contexts and managed category profiles definition and
	 * Initialize these elements.
	 * @throws ChannelConfigException 
	 * @throws ContextNotFoundException 
	 * @throws ManagedCategoryProfileNotFoundException 
	 */
	synchronized private void loadConfig() throws ChannelConfigException, ManagedCategoryProfileNotFoundException, ContextNotFoundException {
		if (log.isDebugEnabled()){
			log.debug("loadConfig()");
		}
		
		try {
			//ChannelConfig config = 
			ChannelConfig.getInstance(configFilePath);
			// TODO (GB later)
			// - utiliser l'objet config pour appeler les m�thodes apr�s (reset ...)
			// 		et faire une classe FileToLoad avec ces m�thodes en non static
			// - charger la config via un DAO ?
			
		} catch (ChannelConfigException e) {
			// TODO (GB later)
//			if (configLoaded) {
//				log.error("Unable to load new config : "+e.getMessage());
//			} else {
				String ErrorMsg = "Unable to load config and start initialization : "+e.getMessage(); 
				log.error(ErrorMsg);
				throw new ChannelConfigException(ErrorMsg,e);
//			}
		}
		
		if (ChannelConfig.isModified()){
			/* Reset channel properties loaded from config */
			resetChannelConfigProperties();
			
			/*Loading guest user name */
			ChannelConfig.loadGuestUser();
			
			/* Loading managed category profiles */
			ChannelConfig.loadManagedCategoryProfiles(this);
				
			/* Loading Contexts */
			ChannelConfig.loadContexts(this);
		
			/* Initialize Contexts and ManagedCategoryProfiles links */
			ChannelConfig.initContextManagedCategoryProfilesLinks(this);
		}
		if (!configLoaded){
			configLoaded = true;
		}
		log.info("The channel config is loaded (file "+ChannelConfig.getfilePath()+") in channel");
	}
	
	/**
	 * Initialize every channel properties that are set up by loading channel configuration file
	 */
	synchronized private void resetChannelConfigProperties() {
		if (log.isDebugEnabled()) {
			log.debug("resetChannelConfigProperties()");
		}
		
		// TODO (GB later) UserAttributes.init();
		contextsHash = new Hashtable<String, Context>();
		managedCategoryProfilesHash = new Hashtable<String, ManagedCategoryProfile>();
	}
	
	/**
	 * Load mapping file if is modified since last loading (using MappingFile)
	 * It gets list of mappings used by the channel and 
	 * Initialize these elements.
	 * @throws MappingFileException 
	 */	
	synchronized private void loadMappingFile() throws MappingFileException {
		if (log.isDebugEnabled()){
			log.debug("loadMappingFile()");
		}
		
		try {
			//MappingFile mappingFile = 
			MappingFile.getInstance(mappingFilePath);
			
		} catch (MappingFileException e) {
			// TODO (GB later)
//			if (mappingsLoaded) {
//				log.error("unable to load new mappings : "+e.getMessage());
//			} else {
				String ErrorMsg = "Unable to load mappings and start initialization : "+e.getMessage(); 
				log.error(ErrorMsg);
				throw new MappingFileException(ErrorMsg);
//			}
		}
		
		if (MappingFile.isModified()){
			/* Reset channel properties loaded from config */
			resetMappingFileProperties();
				
			/* Loading Mappings */
			MappingFile.loadMappings(this);
			
			/* Initialize hashs mapping if channel */
			MappingFile.initChannelHashMappings(this);

		}
		if (!mappingsLoaded){
			mappingsLoaded = true;
		}
		log.info("The mapping is loaded (file "+MappingFile.getMappingFilePath()+") in channel");
	}

	/**
	 * Initialize every channel properties that are set up by loading mapping file
	 */
	synchronized private void resetMappingFileProperties(){
		if (log.isDebugEnabled()){
			log.debug("resetMappingFileProperties()");
		}
//		mappingList = new ArrayList<Mapping>();
		mappingHashByDtd = new Hashtable<String,Mapping>();
		mappingHashByXmlns = new Hashtable<String,Mapping>();
		mappingHashByXmlType = new Hashtable<String,Mapping>();
		mappingHashByRootElement = new Hashtable<String,Mapping>();
		mappingHashBySourceURL= new Hashtable<String,Mapping>();
	}

	
	/*
	 *************************** METHODS ************************************/

	/**
	 * return the user profile identified by "userId". 
	 * It takes it from the dao if exists, else, it create a user profile
	 * @param userId : identifient of the user profile
	 * @return the user profile
	 */
	public synchronized UserProfile getUserProfile(final String userId) {
		if (log.isDebugEnabled()) {
			log.debug("getUserProfile(" + userId + ")");
		}
	
		if (userId == null) {
			String mes = "userId not found. Check Channel configuation.";
			log.error(mes);
			throw new ConfigException(mes);
		}
		UserProfile userProfile = userProfilesHash.get(userId);
		if (userProfile == null) {
			userProfile = DomainTools.getDaoService().getUserProfile(userId);
			if (userProfile == null) {
				userProfile = new UserProfile(userId);
				DomainTools.getDaoService().saveUserProfile(userProfile);
			}
			userProfilesHash.put(userId, userProfile);
		} else {
			// Reatach userProfile to hibernate session
			userProfile = DomainTools.getDaoService().refreshUserProfile(userProfile);
		}
		return userProfile;
	}
	
	/**
	 * return the context identified by "contextId".
	 * if it is defined in channel
	 * @param contextId
	 * @return  the context identified by "contextId"
	 * @throws ContextNotFoundException 
	 */
	protected Context getContext(final String contextId) throws ContextNotFoundException {
		if (log.isDebugEnabled()) {
			log.debug("getContext(" + contextId + ")");
		}
		Context context = contextsHash.get(contextId);
		if (context == null) {
			String errorMsg = "Context " + contextId + " is not defined in channel";
			log.error(errorMsg);
			throw new ContextNotFoundException(errorMsg);
		}
		return context;
	}
	
	/**
	 * @param contextId id of the context
	 * @return true if the context is defined in this Channel
	 */
	protected boolean isThereContext(String contextId) {
		if (log.isDebugEnabled()){
			log.debug("getContext("+contextId+")");
		}
		return contextsHash.containsKey(contextId);		
	}
	
	/**
	 * Add a context to this Channel.
	 * @param c context to add
	 */	
	synchronized protected void addContext(Context c) {
		if (log.isDebugEnabled()){
			log.debug("addContext("+c.getId()+")");
		}
		this.contextsHash.put(c.getId(),c);
	}	

	/**
	 * Returns the managed category profile by identified by Id.
	 * @param id the managed category profile Id
	 * @return managedCategoryProfile
	 * @throws ManagedCategoryProfileNotFoundException 
	 */
	protected ManagedCategoryProfile getManagedCategoryProfile(String id) throws ManagedCategoryProfileNotFoundException{
		if (log.isDebugEnabled()){
			log.debug("getManagedCategoryProfile("+id+")");
		}
		ManagedCategoryProfile mcp = managedCategoryProfilesHash.get(id);
		if (mcp == null) {
			String errorMsg = "ManagedCategoryProfile "+id+" is not defined in channel";
			log.error(errorMsg);
			throw new ManagedCategoryProfileNotFoundException(errorMsg);
		}
		return mcp;
	}
	
	/**
	 * Add a managed category profile to this Channel
	 * @param m the managed category profile to add
	 */
	synchronized protected void addManagedCategoryProfile(ManagedCategoryProfile m) {
		if (log.isDebugEnabled()){
			log.debug("addManagedCategoryProfile("+m.getId()+")");
		}
		this.managedCategoryProfilesHash.put(m.getId(),m);
	}
	
	/**
	 * Add a mapping to the list of mappings defined in this Channel
	 * @param m the mapping to add
	 */
	synchronized protected void addMapping(Mapping m){
		if (log.isDebugEnabled()){
			log.debug("addMapping("+m.getRootElement()+")");
		}
		String sourceURL = m.getSourceURL();
		String dtd = m.getDtd();
		String xmlns = m.getXmlns();
		String xmlType = m.getXmlType();
		String rootElement = m.getRootElement();
		
		if (!sourceURL.equals("")) {
			addMappingBySourceURL(m);
		}
		if (!dtd.equals("")) {
			addMappingByDtd(m);
		}
		if (!xmlns.equals("")) {
			addMappingByXmlns(m);
		}
		if (!xmlType.equals("")) {
			addMappingByXmlType(m);
		}
		if (!rootElement.equals("")) {
			addMappingByRootElement(m);
		}
	}
	
	/**
	 * Add a mapping (defined in the channel) to the hash of mappings indexed by its dtd,
	 * @param m the mapping to add
	 * @see Channel#mappingHashByDtd
	 */
	synchronized private void addMappingByDtd(Mapping m) {
		if (log.isDebugEnabled()){
			log.debug("addMappingByDtd("+m.getRootElement()+")");
		}
		this.mappingHashByDtd.put(m.getDtd(),m);
	}
	
	/**
	 * @param dtd
	 * @return mapping associated with dtd
	 */
	protected Mapping getMappingByDtd(String dtd){
		if (log.isDebugEnabled()){
			log.debug("getMappingByDtd("+dtd+")");
		}
		return mappingHashByDtd.get(dtd);
	}

	/**
	 * Add a mapping (defined in the channel) to the hash of mappings indexed by its xmlns
	 * @param m the mapping to add
	 * @see Channel#mappingHashByXmlns
	 */
	synchronized private void addMappingByXmlns(Mapping m) {
		if (log.isDebugEnabled()){
			log.debug("addMappingByXmlns("+m.getRootElement()+")");
		}
		this.mappingHashByXmlns.put(m.getXmlns(),m);
	}	
	/**
	 * @param xmlns
	 * @return mapping associated with xmlns
	 */
	protected Mapping getMappingByXmlns(String xmlns){
		if (log.isDebugEnabled()){
			log.debug("getMappingByXmlns("+xmlns+")");
		}
		return mappingHashByXmlns.get(xmlns);
	}
	
	/**
	 * Add a mapping (defined in the channel) to the hash of mappings indexed by its xmlType
	 * @param m the mapping to add
	 * @see Channel#mappingHashByXmlType
	 */
	synchronized private void addMappingByXmlType(Mapping m) {
		if (log.isDebugEnabled()){
			log.debug("addMappingByXmlType("+m.getRootElement()+")");
		}
		this.mappingHashByXmlType.put(m.getXmlType(),m);
	}	

	/**
	 * @param xmlType
	 * @return mapping associated with xmlType
	 */
	protected Mapping getMappingByXmlType(String xmlType){
		if (log.isDebugEnabled()){
			log.debug("getMappingByXmlType("+xmlType+")");
		}
		return mappingHashByXmlType.get(xmlType);
	}

	/**
	 * Add a mapping (defined in the channel) to the hash of mappings indexed by its RootElement
	 * @param m the mapping to add
	 * @see Channel#mappingHashByRootElement
	 */
	synchronized private void addMappingByRootElement(Mapping m) {
		if (log.isDebugEnabled()){
			log.debug("addMappingByRootElement("+m.getRootElement()+")");
		}
		this.mappingHashByRootElement.put(m.getRootElement(), m);
	}

	/**
	 * @param rootElement
	 * @return mapping associated with rootElement
	 */
	protected Mapping getMappingByRootElement(String rootElement){
		if (log.isDebugEnabled()){
			log.debug("getMappingByRootElement("+rootElement+")");
		}
		return mappingHashByRootElement.get(rootElement);
	}

	/**
	 * Add a mapping (defined in the channel) to the hash of mappings indexed by its sourceURL
	 * @param m the mapping to add
	 * @see Channel#mappingHashBySourceURL
	 */
	synchronized private void addMappingBySourceURL(Mapping m) {
		if (log.isDebugEnabled()){
			log.debug("addMappingBySourceURL("+m.getRootElement()+")");
		}
		this.mappingHashBySourceURL.put(m.getSourceURL(), m);
	}

	/**
	 * @param sourceURL
	 * @return mapping for this sourceURL
	 */
	protected Mapping getMappingBySourceURL(String sourceURL){
		if (log.isDebugEnabled()){
			log.debug("getMappingBySourceURL("+sourceURL+")");
		}
		return mappingHashBySourceURL.get(sourceURL);
	}
//	/**
//	 * Return a string containing channel content : mapping file, contexts, managed category profiles,
//	 * xslt mappings, hash mappings by dtd, Hash mappings by xmlns,Hash mappings by xmlType
//	 * 
//	 * @see java.lang.Object#toString()
//	 */
//	public String toString() {
//		String string = "";
//			
//		/* mappingFile */
//		string += "***************** Mapping File : \n\n";
//		string += mappingFile.toString();
//		string += "\n";
//		
////		/* Contexts */ 
//		string += "***************** Contexts : \n\n";
//		string += contextsHash.toString();
//	    string += "\n";
////				
////		/* Managed categories profiles */
//		string += "***************** Managed categories profiles : \n\n";
//		string += managedCategoryProfilesHash.toString();
//	    string += "\n";
////		
//		/* Xslt mappings*/
//		string += "***************** Xslt mappings : \n\n";
//		string += mappingList.toString();
//		string += "\n";
//		
//		/* Hash to access mappings by dtd */
//		string += "***************** Hash mappings by dtd : \n\n";
//		string += mappingHashByDtd.toString();
//		string += "\n";
//				
//		/* Hash to access mappings by xmlns */
//		string += "***************** Hash mappings by xmlns : \n\n";
//		string += mappingHashByXmlns.toString();      
//	    string += "\n";
//		
//		/* Hash to access mappings by xmlType */
//		string += "***************** Hash mappings by xmlType : \n\n";
//		string += mappingHashByXmlType.toString();      
//	    string += "\n";
//		
////		/* User Profiles connected to the chanel */
////		string += "***************** User profiles : \n\n";
////		string += " later ...";
////        string += "\n";
//		
//        return string;
//	}		

	/* 
	 *************************** ACCESSORS ********************************* */
	
	/**
	 * Returns the hashtable of contexts, indexed by their ids
	 * @return contextsHash
	 * @see Channel#contextsHash
	 */
	protected Hashtable<String,Context> getContextsHash() {
		return contextsHash;
	}
  

	/**
	 * Returns the list of mappings defined in this Channel
	 * @return mappingList
	 * @see Channel#mappingList
	 */
	protected List<Mapping> getMappingList() {
		return mappingList;
	}
	
	/**
	 * Sets the mappings list of this Channel
	 * @param mappingList
	 */
	synchronized protected void setMappingList(List<Mapping> mappingList) {
		this.mappingList = mappingList;
	}

//	/**
//	 * @return daoService
//	 */
//	public DaoService getDaoService() {
//		return daoService;
//	}

	/**
	 * set DaoService
	 * @param daoService
	 */
	synchronized public void setDaoService(DaoService daoService) {
		this.daoService = daoService;
	}

//	/**
//	 * @return the external service
//	 */
//	public ExternalService getExternalService() {
//		return externalService;
//	}

	/**
	 * @param externalService
	 */
	public void setExternalService(ExternalService externalService) {
		this.externalService = externalService;
	}

	/**
	 * @return the file path of the config file
	 */
	public String getConfigFilePath() {
		return configFilePath;
	}

	/**
	 * set the file path of the config file
	 * @param configFilePath 
	 */
	public void setConfigFilePath(String configFilePath) {
		this.configFilePath = configFilePath;
	}

	/**
	 * @return the fil path of the mapping file
	 */
	public String getMappingFilePath() {
		return mappingFilePath;
	}

	/**
	 * set the file path of the mapping file
	 * @param mappingFilePath
	 */
	public void setMappingFilePath(String mappingFilePath) {
		this.mappingFilePath = mappingFilePath;
	}
	
//	/**
//	 * Returns the hashtable of ManagedCategoryProfile, indexed by their ids
//	 * @return managedCategoryProfilesHash
//	 * @see Channel#managedCategoryProfilesHash
//	 */
//	protected Hashtable<String,ManagedCategoryProfile> getManagedCategoryProfilesHash() {
//		return managedCategoryProfilesHash;
//	}
	
//	/**
//	 * Set the Hashtable of managedCategoryProfiles, indexed by their ids
//	 * @param managedCategoryProfilesHash
//	 * @see Channel#managedCategoryProfilesHash
//	 */
//	synchronized public void setManagedCategoryProfilesHash(Hashtable<String,ManagedCategoryProfile> managedCategoryProfilesHash) {
//		this.managedCategoryProfilesHash = managedCategoryProfilesHash;
//	}

//	/**
//	 * Returns the hashtable of UserProfile, indexed by their ids
//	 * @return userProfilesHash
//	 * @see Channel#userProfilesHash
//	 */
//	public Hashtable<String, UserProfile> getUserProfilesHash() {
//		return userProfilesHash;
//	}
	
//	/**
//	 * Set Hashtable of userProfiles, indexed by their ids
//	 * @param userProfilesHash
//	 * @see Channel#userProfilesHash
//	 */
//	synchronized public void setUserProfilesHash(Hashtable<String, UserProfile> userProfilesHash) {
//		this.userProfilesHash = userProfilesHash;
//	}

//	public void setMappingFile(MappingFile m){
//		this.mappingFile = m;
//	}
//	public MappingFile getMappingFile(){
//		return this.mappingFile;
//	}
	

	

}
