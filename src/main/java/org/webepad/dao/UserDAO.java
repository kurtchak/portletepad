package org.webepad.dao;

import java.util.List;

import org.webepad.model.User;

public interface UserDAO {

	public List<User> readUsers();

	public User getUser(Long id);

	public void insert(User user);

	public void update(User user);

	public void delete(User user);

	public User findUser(String name);
}
