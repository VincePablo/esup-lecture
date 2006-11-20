package org.esupportail.lecture.domain;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.esupportail.lecture.dao.DaoService;
import org.esupportail.lecture.domain.beans.CategoryBean;
import org.esupportail.lecture.domain.beans.ContextBean;
import org.esupportail.lecture.domain.beans.UserBean;
import org.esupportail.lecture.domain.model.Channel;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * Class to test calls to facadeService instead of web interface or command-line
 * @author gbouteil
 *
 */
public class DomainTest {
	protected static final Log log = LogFactory.getLog(DomainTest.class); 
	private static FacadeService facadeService;
	/* Controller local variables */
	private static String userId;
	private static String contextId;
	/**
	 * @param args non argumet needed
	 */
	public static void main(String[] args) {
		ClassPathResource res = new ClassPathResource("properties/applicationContext.xml");
		XmlBeanFactory factory = new XmlBeanFactory(res);
		facadeService = (FacadeService)factory.getBean("facadeService");

		testGetConnectedUser();
		testGetContext();
		testGetCategories();
		// TODO compl�ter pour chaque m�thode � tester
	
		
	}

/*
 * M�thodes de Test
 */

	/**
	 * Test of servide "getConnectedUser"
	 */
	private static void testGetConnectedUser() {
		printIntro("getConnectedUser");
		userId = facadeService.getConnectedUserId();
		UserBean user = facadeService.getConnectedUser(userId);
		System.out.println(user.toString());
		System.out.println("\n");
	}
	
	/**
	 * Test of service "getContext"
	 */
	private static void testGetContext() {
		printIntro("getContext");
		contextId = facadeService.getCurrentContextId();
		ContextBean context = facadeService.getContext(userId,contextId);
		System.out.println(context.toString());
		System.out.println("\n");
	}

	/**
	 * Test of service "getCategories"
	 */
	private static void testGetCategories() {
		printIntro("getCategories");
		List<CategoryBean> categories = facadeService.getCategories(userId, contextId);
		for(CategoryBean cat : categories){
			System.out.println("****** Categorie : ");
			System.out.println("       "+cat.toString());
		}
		
	}



	private static void printIntro(String nomService){
		System.out.println("******************************************************");
		System.out.println("Test du service -"+nomService+"- : ");
	}
	
	public FacadeService getFacadeService() {
		return facadeService;
	}

	public void setFacadeService(FacadeService service) {
		DomainTest.facadeService = service;
	}
}