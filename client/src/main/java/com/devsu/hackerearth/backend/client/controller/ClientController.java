package com.devsu.hackerearth.backend.client.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsu.hackerearth.backend.client.model.dto.ClientDto;
import com.devsu.hackerearth.backend.client.model.dto.PartialClientDto;
import com.devsu.hackerearth.backend.client.service.ClientService;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

	private final ClientService clientService;

	public ClientController(ClientService clientService) {
		this.clientService = clientService;
	}

	public ResponseEntity<List<ClientDto>> getAll(){
		// api/clients
		// Get all clients
		return null;
	}

	public ResponseEntity<ClientDto> get(@PathVariable Long id){
		// api/clients/{id}
		// Get clients by id
		return null;
	}

	public ResponseEntity<ClientDto> create(@RequestBody ClientDto clientDto){
		// api/clients
		// Create client
		return null;
	}

	public ResponseEntity<ClientDto> update(@PathVariable Long id, @RequestBody ClientDto clientDto){
		// api/clients/{id}
		// Update client
		return null;
	}

	public ResponseEntity<ClientDto> partialUpdate(@PathVariable Long id, @RequestBody PartialClientDto partialClientDto){
		// api/accounts/{id}
		// Partial update accounts
		return null;
	}

	public ResponseEntity<Void> delete(@PathVariable Long id){
		// api/clients/{id}
		// Delete client
		return null;
	}
}
