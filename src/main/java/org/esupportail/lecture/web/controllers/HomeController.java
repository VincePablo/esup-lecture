/**
 * ESUP-Portail Lecture - Copyright (c) 2006 ESUP-Portail consortium
 * For any information please refer to http://esup-helpdesk.sourceforge.net
 * You may obtain a copy of the licence at http://www.esup-portail.org/license/
 */
package org.esupportail.lecture.web.controllers;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.esupportail.lecture.domain.model.ItemDisplayMode;
import org.esupportail.lecture.domain.model.UserProfile;
import org.esupportail.lecture.exceptions.web.WebException;
import org.esupportail.lecture.web.beans.CategoryWebBean;
import org.esupportail.lecture.web.beans.ContextWebBean;
import org.esupportail.lecture.web.beans.ItemWebBean;
import org.esupportail.lecture.web.beans.SourceWebBean;

/**
 * @author : Raymond 
 */
public class HomeController extends TwoPanesController {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Key used to store the context in virtual session.
	 */
	static final String CONTEXT = "context";
	/**
	 * Log instance.
	 */
	private static final Log LOG = LogFactory.getLog(HomeController.class);
	/**
	 * Display mode for item (all | unread | unreadfirst).
	 */
	private ItemDisplayMode itemDisplayMode = ItemDisplayMode.ALL;
	/**
	 *  item used by t:updateActionListener.
	 */
	private ItemWebBean ualItem;

	/**
	 * Constructor.
	 */
	public HomeController() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * JSF action : toogle item from read to unread and unread to read.
	 * @return JSF from-outcome
	 */
	public String toggleItemReadState() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("In toggleItemReadState");
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("itemID = " + ualItem.getId());
		}
		SourceWebBean selectedSource = getUalSource();
		try {
			UserProfile userProfile = getUserProfile();
			userProfile = getFacadeService().marckItemReadMode(userProfile, 
					selectedSource.getId(), ualItem.getId(), !ualItem.isRead());
			setUserProfile(userProfile);
		} catch (Exception e) {
			throw new WebException("Error in toggleItemReadState", e);
		}
		if (ualItem.isRead()) {
			selectedSource.setUnreadItemsNumber(selectedSource.getUnreadItemsNumber() + 1);
		} else {
			if (selectedSource.getUnreadItemsNumber() > 0) {
				selectedSource.setUnreadItemsNumber(selectedSource.getUnreadItemsNumber() - 1);
			}
		}
		ualItem.setRead(!ualItem.isRead());
		
		return "OK";
	}

	/**
	 * JSF action : toggle Folded State of the selected category.
	 * @return JSF from-outcome
	 */
	public String toggleFoldedState() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("in toggleFoldedState");
		}
		CategoryWebBean selectedCategory = getUalCategory();
		//toggle expanded status
		selectedCategory.setFolded(!selectedCategory.isFolded());
		return "OK";
	}

	/**
	 * JSF action : Change display mode, nothing todo because itemDisplayMode is valued by JSF.
	 * @return JSF from-outcome
	 */
	public String changeItemDisplayMode() {
		CategoryWebBean selectedCategory = getContext().getSelectedCategory();
		try {
			if (selectedCategory != null) {
				List<SourceWebBean> sources = selectedCategory.getSelectedOrAllSources();
				if (sources != null) {
					UserProfile userProfile = getUserProfile();
					for (SourceWebBean sourceWeb : sources) {
						userProfile = getFacadeService().marckItemDisplayMode(userProfile,
								sourceWeb.getId(), itemDisplayMode);
						sourceWeb.setItemDisplayMode(itemDisplayMode);
					}
					setUserProfile(userProfile);
				}
			}
		} catch (Exception e) {
			throw new WebException("Error in changeItemDisplayMode", e);
		}
		return "OK";
	}	

	/**
	 * JSF action : mark all displayed items as read.
	 * @return JSF from-outcome
	 */
	public String markAllItemsAsRead() {
		return toogleAllItemsReadState(true);
	}	

	/**
	 * JSF action : mark all displayed items as not read.
	 * @return JSF from-outcome
	 */
	public String markAllItemsAsNotRead() {
		return toogleAllItemsReadState(false);
	}	

	/**
	 * JSF action : toogle all displayed items read state.
	 * @param read 
	 * @return JSF from-outcome
	 */
	public String toogleAllItemsReadState(boolean read) {
		CategoryWebBean selectedCategory = getContext().getSelectedCategory();
		try {
			if (selectedCategory != null) {
				List<SourceWebBean> sources = selectedCategory.getSelectedOrAllSources();
				if (sources != null) {
					for (SourceWebBean sourceWeb : sources) {
						List<ItemWebBean> items = sourceWeb.getItems();
						if (items != null) {
							UserProfile userProfile = getUserProfile();
							for (ItemWebBean itemWeb : items) {
								userProfile = getFacadeService().marckItemReadMode(userProfile, 
										sourceWeb.getId(), itemWeb.getId(), read);
								itemWeb.setRead(read);
							}
							setUserProfile(userProfile);
						}
						if (read) {
							sourceWeb.setUnreadItemsNumber(0);
						} else {
							sourceWeb.setUnreadItemsNumber(sourceWeb.getItemsNumber());
						}
					}
				}
			}
		} catch (Exception e) {
			throw new WebException("Error in toogleAllItemsReadState", e);
		}
		return "navigationHome";
	}	

	/*
	 * **************** Getter and Setter ****************
	 */


	/**
	 * @return desciption of current selected element (category or source)
	 */
	public String getSelectedElementDescription() {
		String ret = null;
		ContextWebBean ctx = getContext();
		ret = ctx.getDescription();
		CategoryWebBean selectedCategory = ctx.getSelectedCategory();
		if (selectedCategory != null) {
			ret = selectedCategory.getDescription();
		}
		return ret;
	}

	/**
	 * @return Display mode form items
	 */
	@SuppressWarnings("static-access")
	public ItemDisplayMode getItemDisplayMode() {
		ItemDisplayMode ret = itemDisplayMode.ALL;
		if (LOG.isDebugEnabled()) {
			LOG.debug("getItemDisplayMode()=" + itemDisplayMode);
		}
		ContextWebBean currentContext = getContext();
		CategoryWebBean selectedCategory = currentContext.getSelectedCategory();
		if (selectedCategory != null) {
			List<SourceWebBean> sources = selectedCategory.getSelectedOrAllSources();
			if (sources != null) {
				SourceWebBean source = null;
				if (sources.size() > 0) {
					source  = sources.get(0);
				}
				if (source != null) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("getItemDisplayMode() source selected = " + source.getName());
					}
					ret = source.getItemDisplayMode();
				}
			}
		}
		return ret;
	}

	/**
	 * set the item display mode.
	 * @param itemDisplayMode
	 */
	public void setItemDisplayMode(final ItemDisplayMode itemDisplayMode) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("setItemDisplayMode(" + itemDisplayMode + ")");
		}
		this.itemDisplayMode = itemDisplayMode;
	}

	/**
	 * set item from t:updateActionListener in JSF view.
	 * @param item
	 */
	public void setUalItem(final ItemWebBean item) {
		this.ualItem = item;
	}

	/**
	 * @see org.esupportail.lecture.web.controllers.TwoPanesController#getContextName()
	 */
	@SuppressWarnings("static-access")
	@Override
	protected String getContextName() {
		return this.CONTEXT;
	}

	/**
	 * @return ALL value from ItemDisplayMode enumeration for JSF view
	 */
	public ItemDisplayMode getAll() {
		return ItemDisplayMode.ALL;
	}

	/**
	 * @return UNREAD value from ItemDisplayMode enumeration for JSF view
	 */
	public ItemDisplayMode getUnread() {
		return ItemDisplayMode.UNREAD;
	}

	/**
	 * @return UNREADFIRST value from ItemDisplayMode enumeration for JSF view
	 */
	public ItemDisplayMode getUnreadfirt() {
		return ItemDisplayMode.UNREADFIRST;
	}

	/**
	 * @see org.esupportail.lecture.web.controllers.TwoPanesController#getContextKey()
	 */
	@Override
	String getContextKey() {
		return CONTEXT;
	}

}