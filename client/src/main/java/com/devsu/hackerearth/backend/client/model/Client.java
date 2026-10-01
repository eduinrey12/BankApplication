package com.devsu.hackerearth.backend.client.model;

import javax.persistence.Entity;

@Entity
public class Client extends Person {
	private String password;
	private boolean isActive;
}
