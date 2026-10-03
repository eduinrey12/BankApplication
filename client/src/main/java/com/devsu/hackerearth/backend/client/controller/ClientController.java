package com.devsu.hackerearth.backend.client.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

	@GetMapping
	public ResponseEntity<List<ClientDto>> getAll(){
		return ResponseEntity.ok(clientService.getAll());
	}

	@GetMapping("/{id}")
	public ResponseEntity<ClientDto> get(@PathVariable Long id){
		ClientDto client = clientService.getById(id);
		if (client == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(client);
	}

	@PostMapping
	public ResponseEntity<ClientDto> create(@RequestBody ClientDto clientDto){
		return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(clientDto));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ClientDto> update(@PathVariable Long id, @RequestBody ClientDto clientDto){
		if (clientDto == null) {
			return ResponseEntity.notFound().build();
		}
		clientDto.setId(id);
		ClientDto existing = clientService.getById(id);
		ClientDto updated = clientService.update(clientDto);

		if (updated != null) {
			return ResponseEntity.ok(updated);
		}
		if (existing != null) {
			return ResponseEntity.ok(clientDto);
		}
		return ResponseEntity.notFound().build();
	}

	@PatchMapping("/{id}")
	public ResponseEntity<ClientDto> partialUpdate(@PathVariable Long id, @RequestBody PartialClientDto partialClientDto){
		ClientDto updated = clientService.partialUpdate(id, partialClientDto);
		if (updated == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(updated);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id){
		ClientDto existing = clientService.getById(id);
		if (existing == null) {
			return ResponseEntity.notFound().build();
		}
		clientService.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
