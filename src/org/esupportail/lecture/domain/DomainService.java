package org.esupportail.lecture.domain;

import java.util.List;

import org.esupportail.lecture.domain.beans.CategoryBean;
import org.esupportail.lecture.domain.beans.ContextBean;
import org.esupportail.lecture.domain.beans.ItemBean;
import org.esupportail.lecture.domain.beans.SourceBean;
import org.esupportail.lecture.domain.beans.UserBean;
import org.esupportail.lecture.exceptions.FatalException;
import org.esupportail.lecture.exceptions.ServiceException;

/**
 * @author bourges
 * Domain service interface
 */
public interface DomainService {

	/**
	 * @param uid
	 * @return UserBean
	 * @see FacadeService#getConnectedUser(String)
	 */
	UserBean getConnectedUser(String uid);
	/**
	 * @param uid
	 * @param contextId
	 * @return ContextBean
	 * @see FacadeService#getContext(String,String)
	 */
	ContextBean getContext(String uid,String contextId);

	/**
	 * @param uid
	 * @param contextId
	 * @param externalService access to external service 
	 * @return List<CategoryBean>
	 * @throws ServiceException 
	 * @see FacadeService#getVisibleCategories(String, String)
	 */
	List<CategoryBean> getVisibleCategories(String uid,String contextId,ExternalService externalService) throws ServiceException;

	/**
	 * @param categoryId 
	 * @param uid 
	 * @return List<SourceBean>
	 * @see FacadeService#getVisibleSources(String, String)
	 */
	List<SourceBean> getVisibleSources(String uid,String categoryId,ExternalService externalService);

	/**
	 * @param sourceId 
	 * @param uid 
	 * @param externalService access to external service 
	 * @return List<ItemBean>
	 * @see FacadeService#getItems(String, String)
	 */
	List<ItemBean> getItems( String uid,String sourceId,ExternalService externalService);

	/**
	 * @param uid 
	 * @param itemId 
	 * @param sourceId 
	 * @see FacadeService#marckItemasRead(String, String, String)
	 */
	void marckItemasRead(String uid, String sourceId,String itemId);

	/**
	 * @param uid 
	 * @param itemId 
	 * @param sourceId 
	 * @see FacadeService#marckItemasUnread(String, String, String)
	 */
	void marckItemasUnread(String uid, String sourceId,String itemId);

}
