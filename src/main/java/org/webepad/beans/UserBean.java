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
	private static Logger log = LoggerFactory.getLogger(UserBean.class);
	private UserDAO userDAO = HibernateDAOFactory.getInstance().getUserDAO();
	private User user;
	
	public UserBean() {
	}

	public User getUser(Long id) {
		return userDAO.getUser(id);
	}
	
	public User getUser(String name) {
		return userDAO.findUser(name);
	}

	public List<User> getUsers() {
		return userDAO.readUsers();
	}

	public void save(User user) {
		userDAO.insert(user);
	}

	public User getUser() {
		return user;
	}
	
	public void setUser(User user) {
		this.user = user;
	}
	
	public User getActualUser() {
		if (user == null) {
			loadLoggedUser();
		}
		return user;
	}
	
	private void loadLoggedUser() {
		PortletRequest req = (PortletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
		String name = req.getRemoteUser();
		if (name == null) {
			user = null;
		} else if (user != null && name.equals(user.getName())) {
			return;
		} else {
			user = getUser(name);
			if (user == null) {
				user = new User(name);
				save(user);
			}
			log.info("LOADED USER: " + user.getName());
		}
	}
}
