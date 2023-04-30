package com.smart.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smart.entities.Contact;
import com.smart.entities.User;

public interface ContactRepository extends JpaRepository<Contact, Integer> {
	
	@Query("select c from Contact c where c.user.id = :id")
	// Pageable has two informations
	// current page number - page
	// total no of pages
	public Page<Contact> findContactsByUser(@Param("id") int uid, Pageable pageable);
	
	// serach
	public List<Contact> findByNameContainingAndUser(String key, User user);
}
