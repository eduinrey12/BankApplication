package com.devsu.hackerearth.backend.client.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.devsu.hackerearth.backend.client.model.dto.ClientDto;
import com.devsu.hackerearth.backend.client.model.dto.PartialClientDto;
import com.devsu.hackerearth.backend.client.repository.ClientRepository;

@Service
public class ClientServiceImpl implements ClientService {

	private final ClientRepository clientRepository;

	public ClientServiceImpl(ClientRepository clientRepository) {
		this.clientRepository = clientRepository;
	}

	@Override
	public List<ClientDto> getAll() {
		// Get all clients
		return null;
	}

	@Override
	public ClientDto getById(Long id) {
		// Get clients by id
		return null;
	}

	@Override
	public ClientDto create(ClientDto clientDto) {
		// Create client
		return null;
	}

	@Override
	public ClientDto update(ClientDto clientDto) {
		// Update client
		return null;
	}

	@Override
    public ClientDto partialUpdate(Long id, PartialClientDto partialClientDto) {
        // Partial update account
		return null;
    }

	@Override
	public void deleteById(Long id) {
		// Delete client
	}
}
