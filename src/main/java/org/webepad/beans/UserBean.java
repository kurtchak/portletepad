package org.webepad.beans;

import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.portlet.PortletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webepad.dao.UserDAO;
import org.webepad.dao.hibernate.HibernateDAOFactory;
import org.webepad.model.User;

@ManagedBean(name = "userBean")
@SessionScoped
public class UserBean {
	private Logger log = LoggerFactory.getLogger(UserBean.class);
	private UserDAO userDAO = HibernateDAOFactory.getInstance().getUserDAO();
	private User user;
	
	public UserBean() {
	}

	private void loadLoggedUser() {
		FacesContext context = FacesContext.getCurrentInstance();
		PortletRequest req = (PortletRequest) context.getExternalContext().getRequest();
		String name = req.getRemoteUser();
		if (name == null) {
			user = null;
			return;
		} else if (user != null && name.equals(user.getName())) {
			return;
		} else {
			user = loadUser(name);
			if (user == null) {
				user = new User(name);
				save(user);
			}
			log.info("LOADED USER: " + user.getName());
		}
	}

	public User getUser(Long id) {
		return userDAO.getUser(id);
	}
	
	private void save(User user) {
		userDAO.insert(user);
	}

	private User loadUser(String name) {
		return userDAO.findUser(name);
	}

	public User getUser() {
		return user;
	}
	
	public void setUser(User user) {
		this.user = user;
	}

	/**
	 * RETRIEVE ALL USERS
	 */
	public List<User> getUsers() {
		return userDAO.readUsers();
	}
	
	public User getActualUser() {
		if (user == null) {
			loadLoggedUser();
		}
		return user;
	}
}
