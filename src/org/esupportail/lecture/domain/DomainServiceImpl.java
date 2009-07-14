/**
* ESUP-Portail Lecture - Copyright (c) 2006 ESUP-Portail consortium
* For any information please refer to http://esup-helpdesk.sourceforge.net
* You may obtain a copy of the licence at http://www.esup-portail.org/license/
*/
package org.esupportail.lecture.domain;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.esupportail.commons.exceptions.ConfigException;
import org.esupportail.commons.services.application.Version;
import org.esupportail.commons.services.authentication.AuthenticationService;
import org.esupportail.commons.services.i18n.I18nService;
import org.esupportail.lecture.domain.beans.CategoryBean;
import org.esupportail.lecture.domain.beans.CategoryDummyBean;
import org.esupportail.lecture.domain.beans.ContextBean;
import org.esupportail.lecture.domain.beans.ItemBean;
import org.esupportail.lecture.domain.beans.SourceBean;
import org.esupportail.lecture.domain.beans.SourceDummyBean;
import org.esupportail.lecture.domain.beans.UserBean;
import org.esupportail.lecture.domain.model.Channel;
import org.esupportail.lecture.domain.model.CoupleProfileAvailability;
import org.esupportail.lecture.domain.model.CustomCategory;
import org.esupportail.lecture.domain.model.CustomContext;
import org.esupportail.lecture.domain.model.CustomSource;
import org.esupportail.lecture.domain.model.Item;
import org.esupportail.lecture.domain.model.ItemDisplayMode;
import org.esupportail.lecture.domain.model.ManagedCategoryDummy;
import org.esupportail.lecture.domain.model.UserProfile;
import org.esupportail.lecture.domain.model.VersionManager;
import org.esupportail.lecture.exceptions.domain.CategoryNotVisibleException;
import org.esupportail.lecture.exceptions.domain.CategoryObligedException;
import org.esupportail.lecture.exceptions.domain.CategoryProfileNotFoundException;
import org.esupportail.lecture.exceptions.domain.CategoryTimeOutException;
import org.esupportail.lecture.exceptions.domain.ComputeItemsException;
import org.esupportail.lecture.exceptions.domain.ContextNotFoundException;
import org.esupportail.lecture.exceptions.domain.CustomCategoryNotFoundException;
import org.esupportail.lecture.exceptions.domain.CustomSourceNotFoundException;
import org.esupportail.lecture.exceptions.domain.DomainServiceException;
import org.esupportail.lecture.exceptions.domain.InfoDomainException;
import org.esupportail.lecture.exceptions.domain.InternalDomainException;
import org.esupportail.lecture.exceptions.domain.ManagedCategoryNotLoadedException;
import org.esupportail.lecture.exceptions.domain.SourceNotLoadedException;
import org.esupportail.lecture.exceptions.domain.SourceNotVisibleException;
import org.esupportail.lecture.exceptions.domain.SourceObligedException;
import org.esupportail.lecture.exceptions.domain.SourceProfileNotFoundException;
import org.esupportail.lecture.exceptions.domain.TreeSizeErrorException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.util.Assert;

/**
 * Service implementation provided by domain layer
 * All of services are available for a user only if 
 * he has a customContext defined in his userProfile.
 * To have a customContext defined in a userProfile, the service
 * getContext must have been called one time (over several user session)
 * This class throws ContextNotFoundException because getting context is not an
 * automatic research, it is leading by higher layer.
 * @author gbouteil
 */
public class DomainServiceImpl implements DomainService, InitializingBean {	
	/*
	 ************************** PROPERTIES ******************************** */	
	
	/** 
	 * Main domain model class.
	 */
	private static Channel channel; 
	
	/**
	 * Log instance.
	 */
	private static final Log LOG = LogFactory.getLog(DomainServiceImpl.class);

	/**
	 * The authentication Service.
	 */
	private AuthenticationService authenticationService;

	/**
	 * The i18n Service.
	 */
	private I18nService i18nService;

	/* 
	 ************************** INIT **********************************/

	/**
	 * default constructor.
	 */
	public DomainServiceImpl() {
		super();
	}


	/**
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() {
		Assert.notNull(channel, "property channel of class "
				+ this.getClass().getName() + " can not be null");
		Assert.notNull(authenticationService, "property authenticationService of class " 
				+ this.getClass().getName() + " can not be null");
		Assert.notNull(i18nService, "property i18nService of class " 
				+ this.getClass().getName() + " can not be null");
	}

	
	/*
	 ************************** Methodes - services - mode NORMAL ************************************/

	/**
	 * Return the user identified by userId.
	 * @param userId user Id
	 * @return userBean
	 * @see org.esupportail.lecture.domain.DomainService#getConnectedUser(java.lang.String)
	 */
	public UserBean getConnectedUser(final String userId) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("getConnectedUser(" + userId + ")");
		}
		
		/* User profile creation */
		UserProfile userProfile = channel.getUserProfile(userId);
		
		/* userBean creation */
		UserBean user = new UserBean(userProfile);
		
		return user;
	}

	/**
	 * Returns the contextBean corresponding to the context identified by contextId for user userId.
	 * @param userId id of the current user
	 * @param contextId id of the context to get
	 * @return contextBean
	 * @throws InternalDomainException 
	 * @see org.esupportail.lecture.domain.DomainService#getContext(String,String)
	 */
	public ContextBean getContext(final String userId, final String contextId) throws InternalDomainException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("getContext(" + userId + "," + contextId + ")");
		}
		
		ContextBean contextBean;
		/* Get current user profile and customContext */
		UserProfile userProfile = channel.getUserProfile(userId);
		try {
			CustomContext customContext = userProfile.getCustomContext(contextId);
			
			/* Make the contextUserBean to display */
			contextBean = new ContextBean(customContext);
		} catch (ContextNotFoundException e) {
			String errorMsg = "Unable to getContext because context is not found";
			LOG.error(errorMsg);
			throw new InternalDomainException(errorMsg, e);
		}
		
		return contextBean;		
	}

	/**
	 * Returns a list of categoryBean - corresponding to categories to display on interface.
	 * into context contextId for user userId
	 * Displayed categories are one that user : 
	 * - is subscribed to (obliged or allowed or autoSubscribe)
	 * - has created (personal categories)
	 * @param userId id of the current user
	 * @param contextId  id of the current context 
	 * @return a list of CategoryBean
	 * @throws InternalDomainException
	 * @see org.esupportail.lecture.domain.DomainService#getDisplayedCategories(String,String)
	 */
	public List<CategoryBean> getDisplayedCategories(final String userId, final String contextId) 
	throws InternalDomainException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("getDisplayedCategories(" + userId + "," + contextId + ")");
		}
		
		/* Get current user profile and customContext */
		UserProfile userProfile = channel.getUserProfile(userId);
		CustomContext customContext;
		try {
			customContext = userProfile.getCustomContext(contextId);
		} catch (ContextNotFoundException e1) {
			String errorMsg = "Unable to getDisplayedCategories because context is not found";
			LOG.error(errorMsg);
			throw new InternalDomainException(errorMsg, e1);
		}

		List<CategoryBean> listCategoryBean = new ArrayList<CategoryBean>();
		List<CustomCategory> customCategories = customContext.getSortedCustomCategories();
		for (CustomCategory customCategory : customCategories) {
			CategoryBean category;
			try {
				category = new CategoryBean(customCategory, customContext);
				if (category instanceof CategoryDummyBean) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("CategoryDummyBean created : "+category.getId());
					}
				}
				listCategoryBean.add(category);
			} catch (CategoryProfileNotFoundException e) {
				LOG.warn("Warning on service 'getDisplayedeCategories(user " 
					+ userId + ", context " + contextId + ") : clean custom source ");
				//userProfile.cleanCustomCategoryFromProfile(customCategory.getElementId());
				userProfile.removeCustomCategoryFromProfile(customCategory.getElementId());
			} catch (InfoDomainException e) {
				LOG.error("Error on service 'getDisplayedCategories(user " 
					+ userId + ", context " + contextId + ") : creation of a CategoryDummyBean");
				category = new CategoryDummyBean(e);
				listCategoryBean.add(category);
			} 

		}
		return listCategoryBean;
	}
	
	/**
	 * Returns a list of sourceBean - corresponding to available categories to display on interface.
	 * into category categoryId for user userId
	 * Available sources are one that user : 
	 * - is subscribed to (obliged or allowed or autoSubscribe)
	 * - has created (personal sources)
	 * @param uid Id of the user
	 * @param categoryId id of the category to display sources
	 * @return a list of sourceBean
	 * @throws InternalDomainException 
	 * @throws CategoryNotVisibleException 
	 * @throws CategoryTimeOutException 
	 * @see org.esupportail.lecture.domain.DomainService#getDisplayedSources(
	 *   java.lang.String, java.lang.String)
	 */
	public List<SourceBean> getDisplayedSources(final String uid, final String categoryId) 
	throws InternalDomainException, CategoryNotVisibleException, CategoryTimeOutException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("getDisplayedSources(" + uid + "," + categoryId + ")");
		}
		
		List<SourceBean> listSourceBean = new ArrayList<SourceBean>();
		UserProfile userProfile = channel.getUserProfile(uid);
		try {
			CustomCategory customCategory = userProfile.getCustomCategory(categoryId);
			List<CustomSource> customSources = customCategory.getSortedCustomSources();
				
			for (CustomSource customSource : customSources) {
				SourceBean source;
				try {
					source = new SourceBean(customSource);
					if (source instanceof SourceDummyBean) {
						if (LOG.isDebugEnabled()) {
							LOG.debug("SourceDummyBean created : "+source.getId());
						}
					}
					listSourceBean.add(source);
// GB : No needed because of improve of exception management					
//				} catch (SourceProfileNotFoundException e) {
//					LOG.warn("Warning on service 'getDisplayedSources(user " 
//						+ uid + ", category " + categoryId + ") : clean custom source ");
//					userProfile.removeCustomSourceFromProfile(customSource.getElementId());
				} catch (InfoDomainException e) {
					LOG.error("Error on service 'getDisplayedSources(user "
						+ uid + ", category " + categoryId + ") : " 
						+ "creation of a SourceDummyBean");
					source = new SourceDummyBean(e);
					listSourceBean.add(source);
				} catch (DomainServiceException e) {
					LOG.error("Error on service 'getDisplayedSources(user "
						+ uid + ", category " + categoryId + ") : " 
						+ "creation of a SourceDummyBean");
					source = new SourceDummyBean(e);
					listSourceBean.add(source);
				}
			}
// GB : No needed because of improve of exception management	
//		} catch	(CategoryProfileNotFoundException e) {
//			String errorMsg = "CategoryProfileNotFoundException for service 'getDisplayedSources(user "
//				+ uid + ", category " + categoryId + ")";
//			LOG.error(errorMsg);
//			//userProfile.cleanCustomCategoryFromProfile(categoryId);
//			userProfile.removeCustomCategoryFromProfile(categoryId);
//			throw new InternalDomainException(errorMsg, e);
		} catch (CustomCategoryNotFoundException e) {
			String errorMsg = "CustomCategoryNotFound for service 'getDisplayedSources(user "
				+ uid + ", category " + categoryId + ")\n" 
				+ "User " + uid + " is not subscriber of Category " + categoryId;
			LOG.error(errorMsg);
			// TODO (GB RB) Remonter une SubsriptionNotFoundForUserException à la place ?
			throw new InternalDomainException(errorMsg, e);
		}
		
		return listSourceBean;
	}

	/** 
	 * Returns a list of itemBean.
	 * Corresponding to items containing in source sourceId,
	 * in order to be displayed on user interface for user uid
	 * @param uid user Id
	 * @param sourceId source Id to display items
	 * @return a list of itemBean
	 * @throws InternalDomainException 
	 * @throws ManagedCategoryNotLoadedException 
	 * @throws SourceNotLoadedException 
	 * @see org.esupportail.lecture.domain.DomainService#getItems(
	 *   java.lang.String, java.lang.String)
	 */
	public List<ItemBean> getItems(final String uid, final String sourceId) 
	throws InternalDomainException, SourceNotLoadedException, ManagedCategoryNotLoadedException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("getItems(" + uid + "," + sourceId + ")");
		}
		
		List<ItemBean> listItemBean = new ArrayList<ItemBean>();
		UserProfile userProfile = channel.getUserProfile(uid);
		CustomSource customSource = null;
		try {
			/* Get current user profile and customCoategory */
			customSource = userProfile.getCustomSource(sourceId);
			List<Item> listItems;
			listItems = customSource.getItems();

			for (Item item : listItems) {
				ItemBean itemBean = new ItemBean(item, customSource);
				listItemBean.add(itemBean);
			}
// GB : No needed because of improve of exception management	
//		} catch (ManagedCategoryProfileNotFoundException e) {
//			String errorMsg = "ManagedCategoryProfileNotFoundException for service 'getItems(user "
//				+ uid + ", source " + sourceId + ")";
//			LOG.error(errorMsg);
//			CustomManagedSource customManagedSource = (CustomManagedSource) customSource;
//			String categoryId = customManagedSource.getManagedSourceProfileParentId();
//			//userProfile.cleanCustomCategoryFromProfile(categoryId);
//			userProfile.removeCustomCategoryFromProfile(categoryId);
//			throw new InternalDomainException(errorMsg, e);
//		} catch	(SourceProfileNotFoundException e) {
//			String errorMsg = "SourceProfileNotFoundException for service 'getItems(user "
//				+ uid + ", source " + sourceId + ")";
//			LOG.error(errorMsg);
//			userProfile.removeCustomSourceFromProfile(sourceId);
//			throw new InternalDomainException(errorMsg, e);
		} catch (CustomSourceNotFoundException e) {
			String errorMsg = "CustomSourceNotFoundException for service 'getItems(user "
				+ uid + ", source " + sourceId + ")";
			LOG.error(errorMsg);
			// TODO (GB RB) Remonter une SubsriptionNotFoundForUserException à la place ?
			throw new InternalDomainException(errorMsg, e);
		} catch (ComputeItemsException e) {
			String errorMsg = "ComputeItemsException for service 'getItems(user "
				+ uid + ", source " + sourceId + ")";
			LOG.error(errorMsg);
			throw new InternalDomainException(errorMsg, e);
		} 		
		return listItemBean;
	}

	/**
	 * Mark item as read for user uid.
	 * @param uid user Id
	 * @param sourceId sourceId of the item
	 * @param itemId item Id
	 * @param isRead the read Mode (true=item read | false=item not read)
	 * @throws InternalDomainException 
	 * @see org.esupportail.lecture.domain.DomainService#marckItemReadMode(
	 *   java.lang.String, java.lang.String, java.lang.String, boolean)
	 */
	public void marckItemReadMode(final String uid, final String sourceId, 
		final String itemId, final boolean isRead) 
	throws InternalDomainException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("marckItemReadMode(" + uid + "," + sourceId + "," + itemId + "," + isRead + ")");
		}
		
		try {
			/* Get current user profile and customCoategory */
			UserProfile userProfile = channel.getUserProfile(uid);
			CustomSource customSource;
			customSource = userProfile.getCustomSource(sourceId);
			customSource.setItemReadMode(itemId, isRead);
		} catch (CustomSourceNotFoundException e) {
			String errorMsg = "CustomSourceNotFoundException for service 'marckItemReadMode(user "
				+ uid + ", source " + sourceId + ", item " + itemId + ", isRead " + isRead + ")";
			LOG.error(errorMsg);
			// TODO (GB RB) Remonter une SubsriptionNotFoundForUserException à la place ?
			throw new InternalDomainException(errorMsg, e);
		}
		
	}
	
	
	/**
	 * Mark item display mode on source for a user.
	 * @param uid user ID
	 * @param sourceId source ID
	 * @param mode item display mode to set
	 * @throws InternalDomainException 
	 * @see DomainService#markItemDisplayMode(String, String, ItemDisplayMode)
	 */
	public void markItemDisplayMode(final String uid, final String sourceId, 
			final ItemDisplayMode mode) throws InternalDomainException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("markItemDisplayMode(" + uid + "," + sourceId + "," + mode + ")");
		}
		
		try {
			/* Get current user profile and customCategory */
			UserProfile userProfile = channel.getUserProfile(uid);
			CustomSource customSource;
			customSource = userProfile.getCustomSource(sourceId);
			customSource.modifyItemDisplayMode(mode);
		} catch (CustomSourceNotFoundException e) {
			String errorMsg = "CustomSourceNotFoundException for service 'markItemDisplayMode(user "
				+ uid + ", source " + sourceId + ", mode " + mode + ")";
			LOG.error(errorMsg);
			// TODO (GB RB) Remonter une SubsriptionNotFoundForUserException à la place ?
			throw new InternalDomainException(errorMsg, e);
		}
		
	}
	
	
	/**
	 * Set the tree size of the customContext.
	 * @param uid user Id for user uid
	 * @param contextId context Id
	 * @param size size to set
	 * @throws InternalDomainException 
	 * @throws TreeSizeErrorException 
	 * @see org.esupportail.lecture.domain.DomainService#setTreeSize(java.lang.String, java.lang.String, int)
	 */
	public void setTreeSize(final String uid, final String contextId, final int size) 
	throws InternalDomainException, TreeSizeErrorException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("setTreeSize(" + uid + "," + contextId + "," + size + ")");
		}
		/* Get current user profile and customContext */
		UserProfile userProfile = channel.getUserProfile(uid);
		CustomContext customContext;
		try {
			customContext = userProfile.getCustomContext(contextId);
		} catch (ContextNotFoundException e) {
			String errorMsg = "Unable to setTreeSize because context is not found";
			LOG.error(errorMsg);
			throw new InternalDomainException(errorMsg, e);
		}
		customContext.modifyTreeSize(size);
	}

	/**
	 * Set category identified by catId as fold in the customContext ctxId.
	 * for user uid
	 * @param uid user Id
	 * @param cxtId context Id 
	 * @param catId category Id
	 * @throws InternalDomainException 
	 * @see org.esupportail.lecture.domain.DomainService#foldCategory(
	 *   java.lang.String, java.lang.String, java.lang.String)
	 */
	public void foldCategory(final String uid, final String cxtId, final String catId) 
	throws InternalDomainException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("foldCategory(" + uid + "," + cxtId + "," + catId + ")");
		}
		
		/* Get current user profile and customContext */
		UserProfile userProfile = channel.getUserProfile(uid);
		CustomContext customContext;
		try {
			customContext = userProfile.getCustomContext(cxtId);
		} catch (ContextNotFoundException e) {
			String errorMsg = "Unable to foldCategory because context is not found";
			LOG.error(errorMsg);
			throw new InternalDomainException(errorMsg, e);
		}
		customContext.foldCategory(catId);
	}
	
	/**
	 * Set category identified by catId as unfold in the customContext ctxId.
	 * for user uid
	 * @param uid user Id
	 * @param cxtId context Id 
	 * @param catId category Id
	 * @throws InternalDomainException 
	 * @see org.esupportail.lecture.domain.DomainService#unfoldCategory(
	 *   java.lang.String, java.lang.String, java.lang.String)
	 */
	public void unfoldCategory(final String uid, final String cxtId, final String catId) 
	throws InternalDomainException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("unfoldCategory(" + uid + "," + cxtId + "," + catId + ")");
		}
		
		/* Get current user profile and customContext */
		UserProfile userProfile = channel.getUserProfile(uid);
		CustomContext customContext;
		try {
			customContext = userProfile.getCustomContext(cxtId);
		} catch (ContextNotFoundException e) {
			String errorMsg = "Unable to unfoldCategory because context is not found";
			LOG.error(errorMsg);
			throw new InternalDomainException(errorMsg, e);
		}
		customContext.unfoldCategory(catId);
	}
	
	/*
	 ************************** Methodes - services - mode EDIT ************************************/
	
	/**
	 * @throws InternalDomainException 
	 * @throws ManagedCategoryNotLoadedException 
	 * @see org.esupportail.lecture.domain.DomainService#getVisibleCategories(
	 *   java.lang.String, java.lang.String)
	 */
	public List<CategoryBean> getVisibleCategories(final String uid, final String contextId) 
	throws InternalDomainException, ManagedCategoryNotLoadedException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("getVisibleCategories(" + uid + "," + contextId + ")");
		}
		List<CategoryBean> listCategoryBean = new ArrayList<CategoryBean>();
		UserProfile userProfile = channel.getUserProfile(uid);
		CustomContext customContext;
		try {
			customContext = userProfile.getCustomContext(contextId);
		} catch (ContextNotFoundException e) {
			String errorMsg = "Unable to getVisibleCategories because context is not found";
			LOG.error(errorMsg);
			throw new InternalDomainException(errorMsg, e);
		}
		List<CoupleProfileAvailability> couples = customContext.getVisibleCategories();
		for (CoupleProfileAvailability couple : couples) {
			CategoryBean category;
			category = new CategoryBean(couple);
			listCategoryBean.add(category);
		}	
		return listCategoryBean;

	}

	/**
	 * @throws InternalDomainException 
	 * @throws CategoryNotVisibleException 
	 * @throws CategoryTimeOutException 
	 * @see org.esupportail.lecture.domain.DomainService#getVisibleSources(
	 *   java.lang.String, java.lang.String)
	 */
	public List<SourceBean> getVisibleSources(final String uid, final String categoryId) 
	throws CategoryNotVisibleException, InternalDomainException, CategoryTimeOutException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("getVisibleSources(" + uid + "," + categoryId + ")");
		}
		List<SourceBean> listSourceBean = new ArrayList<SourceBean>();
		UserProfile userProfile = channel.getUserProfile(uid);
		try {
			
			CustomCategory customCategory = userProfile.getCustomCategory(categoryId);
			List<CoupleProfileAvailability> couples = customCategory.getVisibleSources();
			for (CoupleProfileAvailability couple : couples) {
				SourceBean source;
//				try {
					source = new SourceBean(couple);
					listSourceBean.add(source);
// GB : No needed because of improve of exception management	
//				}catch (InfoDomainException e) {
//					LOG.error("Error on service 'getVisibleSources(user "
//						+uid+", category "+categoryId+") : creation of a SourceDummyBean");
//					source = new SourceDummyBean(e);
//					listSourceBean.add(source);
//				}
			}	
// GB : No needed because of improve of exception management	
//		} catch	(CategoryProfileNotFoundException e) {
//			String errorMsg = "CategoryProfileNotFoundException for service 'getVisibleSources(user "
//				+ uid + ", category " + categoryId + ")";
//			LOG.error(errorMsg);
//			//userProfile.cleanCustomCategoryFromProfile(categoryId);
//			userProfile.removeCustomCategoryFromProfile(categoryId);
//			throw new InternalDomainException(errorMsg, e);
		} catch (CustomCategoryNotFoundException e) {
			String errorMsg = "CustomCategoryNotFound for service 'getVisibleSources(user " 
				+ uid + ", category " + categoryId + ")" 
				+ "User " + uid + " is not subscriber of Category " + categoryId;
			LOG.error(errorMsg);
			// TODO (GB RB) Remonter une SubsriptionNotFoundForUserException à la place ?
			throw new InternalDomainException(errorMsg, e);
		}
		 return listSourceBean;
		
	}
	
	/**
	 * subscribe user uid to category categoryId in context contextId.
	 * @param uid user ID
	 * @param contextId context ID
	 * @param categoryId category ID
	 * @throws InternalDomainException 
	 * @throws CategoryNotVisibleException 
	 * 
	 */
	public void subscribeToCategory(final String uid, final String contextId, final String categoryId) 
	throws InternalDomainException, CategoryNotVisibleException  {
		if (LOG.isDebugEnabled()) {
			LOG.debug("subscribeToCategory(" + uid + "," + contextId 
				+ "," + categoryId + ")");
		}
		UserProfile userProfile = channel.getUserProfile(uid);
		
		CustomContext customContext;
		try {
			customContext = userProfile.getCustomContext(contextId);
		} catch (ContextNotFoundException e) {
			String errorMsg = "Unable to subscribeToCategory because context is not found";
			LOG.error(errorMsg);
			throw new InternalDomainException(errorMsg, e);
		}
		customContext.subscribeToCategory(categoryId);
	}

	

	/**
	 * subscribe user uid to source sourceId in categoryId, if user is already subscriber of categoryId.
	 * @param uid user ID
	 * @param categoryId category ID
	 * @param sourceId source ID
	 * @throws InternalDomainException 
	 * @throws CategoryNotVisibleException 
	 * @throws SourceNotVisibleException 
	 * @throws CategoryTimeOutException 
	 */
	public void subscribeToSource(final String uid, final String categoryId, final String sourceId) 
	throws CategoryNotVisibleException, InternalDomainException, 
	CategoryTimeOutException, SourceNotVisibleException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("subscribeToSource(" + uid + "," + categoryId 
				+ "," + sourceId + ")");
		}
		UserProfile userProfile = channel.getUserProfile(uid);
		try {
			CustomCategory customCategory = userProfile.getCustomCategory(categoryId);
			customCategory.subscribeToSource(sourceId);
// GB : No needed because of improve of exception management	
//			} catch	(CategoryProfileNotFoundException e) {
//			String errorMsg = "CategoryProfileNotFoundException for service 'subscribeToSource(user "
//				+ uid + ", category " + categoryId + ", source " + sourceId + ")";
//			LOG.error(errorMsg);
//			//userProfile.cleanCustomCategoryFromProfile(categoryId);
//			userProfile.removeCustomCategoryFromProfile(categoryId);
//			throw new InternalDomainException(errorMsg, e);
		} catch (CustomCategoryNotFoundException e) {
			String errorMsg = "CustomCategoryNotFound for service 'subscribeToSource(user "
				+ uid + ", category " + categoryId + ", source " + sourceId + ").\n" 
				+ "User " + uid + " is not subscriber of Category " + categoryId;
			LOG.error(errorMsg);
			// TODO (GB RB) Remonter une SubsriptionNotFoundForUserException à la place ?
			throw new InternalDomainException(errorMsg, e);
		} 
	}
	
	/**
	 * unsubscribe user uid to category categoryId in context contextId.
	 * @param uid user ID
	 * @param contextId context ID
	 * @param categoryId category ID
	 * @throws InternalDomainException 
	 * @throws CategoryObligedException 
	 * @throws CategoryNotVisibleException 
	 * 
	 */
	public void unsubscribeToCategory(final String uid, final String contextId, final String categoryId) 
	throws InternalDomainException, CategoryNotVisibleException, CategoryObligedException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("unsubscribeToCategory(" + uid + "," + contextId + "," + categoryId + ")");
		}
		UserProfile userProfile = channel.getUserProfile(uid);			
		CustomContext customContext;
		try {
			customContext = userProfile.getCustomContext(contextId);
		} catch (ContextNotFoundException e) {
			String errorMsg = "Unable to unsubscribeToCategory because context is not found";
			LOG.error(errorMsg);
			throw new InternalDomainException(errorMsg, e);
		}
		customContext.unsubscribeToCategory(categoryId);
	}

	/**
	 * unsubscribe user uid to source sourceId in categoryId, if user is already subscriber of categoryId.
	 * @param uid user ID
	 * @param categoryId category ID
	 * @param sourceId source ID
	 * @throws InternalDomainException 
	 * @throws CategoryNotVisibleException 
	 * @throws SourceObligedException 
	 * @throws CategoryTimeOutException 
	 */
	public void unsubscribeToSource(final String uid, final String categoryId, final String sourceId) 
	throws InternalDomainException, CategoryNotVisibleException, CategoryTimeOutException, SourceObligedException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("subscribeToSource(" + uid + "," + categoryId + "," 
				+ sourceId + ")");
		}
		UserProfile userProfile = channel.getUserProfile(uid);
		try {
			CustomCategory customCategory = userProfile.getCustomCategory(categoryId);
			customCategory.unsubscribeToSource(sourceId);
// GB : No needed because of improve of exception management	
//		} catch	(CategoryProfileNotFoundException e) {
//			String errorMsg = "CategoryProfileNotFoundException for service 'unsubscribeToSource(user "
//				+ uid + ", category " + categoryId + ", source " + sourceId + ")";
//			LOG.error(errorMsg);
//			//userProfile.cleanCustomCategoryFromProfile(categoryId);
//			userProfile.removeCustomCategoryFromProfile(categoryId);
//			throw new InternalDomainException(errorMsg, e);
		} catch (CustomCategoryNotFoundException e) {
			String errorMsg = "CustomCategoryNotFound for service 'unsubscribeToSource(user "
				+ uid + ", category " + categoryId + ", source " + sourceId + ").\n" 
				+ "User " + uid + " is not subscriber of Category " + categoryId;
			LOG.error(errorMsg);
			// TODO (GB RB) Remonter une SubsriptionNotFoundForUserException à la place ?
			throw new InternalDomainException(errorMsg, e);
		} 	
	}
	
	
	

	/*
	 ************************** Accessors ************************************/
	
	/**
	 * @return channel
	 */
	public Channel getChannel() {
		// It could be static without spring 
		return channel;
	}

	/**
	 * @param channel
	 */
	public void setChannel(final Channel channel) {
		// It could be static without spring 
		DomainServiceImpl.channel = channel;
	}


	/**
	 * @see org.esupportail.lecture.domain.DomainService#getDatabaseVersion()
	 */
	public Version getDatabaseVersion() throws ConfigException {
		VersionManager versionManager = getVersionManager();
		if (versionManager == null) {
			return null;
		}
		return new Version(versionManager.getVersion());
	}

	/**
	 * @see org.esupportail.lecture.domain.DomainService#setDatabaseVersion(java.lang.String)
	 */
	public void setDatabaseVersion(final String version) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("setting database version to '" + version + "'...");
		}
		VersionManager versionManager = getVersionManager();
		if (versionManager == null) {
			versionManager = new VersionManager();
			versionManager.setVersion(version);
			DomainTools.getDaoService().addVersionManager(versionManager);
		} else {
			versionManager.setVersion(version);
			DomainTools.getDaoService().updateVersionManager(versionManager);
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("database version set to '" + version + "'.");
		}
	}


	/**
	 * @return the first (and only) VersionManager instance of the database.
	 * @throws ConfigException 
	 */
	private VersionManager getVersionManager() throws ConfigException {
		List<VersionManager> versionManagers = null;
		try {
			versionManagers = DomainTools.getDaoService().getVersionManagers();
		} catch (BadSqlGrammarException e) {
			throw new ConfigException("your database is not initialized, please run 'ant init'", e);
		}
		if (versionManagers.isEmpty()) {
			return null;
		}
		return versionManagers.get(0);
	}


	/**
	 * @see org.esupportail.lecture.domain.DomainService#isGuestMode()
	 */
	public boolean isGuestMode() {
		boolean ret;
		String connectedUser = authenticationService.getAuthInfo().getId();
		if (connectedUser == null) {
			return true;
		}
		if (connectedUser.equals(DomainTools.getGuestUser())) {
			ret = true;
		} else {
			ret = false;
		}
		return ret;
	}

	/*
	 *************************** ACCESSORS ********************************* */	
	
	/**
	 * @param authenticationService the authenticationService to set
	 */
	public void setAuthenticationService(final AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}


	/**
	 * @param service the i18nService to set
	 */
	public void setI18nService(final I18nService service) {
		i18nService = service;
	}

}
