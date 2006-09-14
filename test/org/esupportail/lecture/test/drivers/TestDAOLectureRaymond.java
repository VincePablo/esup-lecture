/**
* ESUP-Portail Lecture - Copyright (c) 2006 ESUP-Portail consortium
* For any information please refer to http://esup-helpdesk.sourceforge.net
* You may obtain a copy of the licence at http://www.esup-portail.org/license/
*/
package org.esupportail.lecture.test.drivers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.esupportail.lecture.dao.DaoService;
import org.esupportail.lecture.dao.hibernate.DaoServiceHibernate;
import org.esupportail.lecture.domain.model.Channel;
import org.esupportail.lecture.domain.model.CustomContext;
import org.esupportail.lecture.domain.model.UserProfile;
import org.esupportail.lecture.domain.service.DomainService;

import java.io.IOException;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * Stub to test classes in package org.esupportail.domain.model
 * @author gbouteil
 * @see org.esupportail.lecture.domain.model
 */
public class TestDAOLectureRaymond {
	
	protected static final Log log = LogFactory.getLog(Channel.class); 
	
	/**
	 * @param args non argumet needed
	 */
	public static void main(String[] args) {
	
		ClassPathResource res = new ClassPathResource("applicationContextRaymond.xml");
		XmlBeanFactory factory = new XmlBeanFactory(res);
		
		// get one UserProfile
		DaoService dao = (DaoService)factory.getBean("daoServiceHibernate");
		UserProfile userProfile = dao.getUserProfile("bourges");
		System.out.println("userProfile.getUserId --> " + userProfile.getUserId());
		// set and get an other UserProfile
		userProfile = new UserProfile();
		userProfile.setUserId("test");
		dao.addUserProfile(userProfile);
		userProfile = dao.getUserProfile("test");
		System.out.println("userProfile.getUserId --> " + userProfile.getUserId());
//		CustomContext customContext = dao.getCustomContext(1);
//		System.out.println("customContext.getContextId --> " + customContext.getContextId());
		
		
		
	}
}