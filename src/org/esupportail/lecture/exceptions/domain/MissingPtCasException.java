/**
* ESUP-Portail Lecture - Copyright (c) 2006 ESUP-Portail consortium
* For any information please refer to http://esup-helpdesk.sourceforge.net
* You may obtain a copy of the licence at http://www.esup-portail.org/license/
*/
package org.esupportail.lecture.exceptions.domain;


/**
 * 
 * @author gbouteil
 *
 */
public class MissingPtCasException extends InfoDomainException {

	public MissingPtCasException(Exception cause) {
		super(cause);
	}
	public MissingPtCasException(String message) {
		super(message);
	}

	

}