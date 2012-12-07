package org.webepad.beans;

import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.webepad.dao.UserDAO;
import org.webepad.dao.hibernate.HibernateDAOFactory;
import org.webepad.model.User;

@ManagedBean(name = "userBean")
@SessionScoped
public class UserBean {
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
}
