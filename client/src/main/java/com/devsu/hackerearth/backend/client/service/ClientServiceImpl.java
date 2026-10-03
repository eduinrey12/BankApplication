package com.devsu.hackerearth.backend.client.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.devsu.hackerearth.backend.client.exception.ResourceNotFoundException;
import com.devsu.hackerearth.backend.client.model.Client;
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
		return clientRepository.findAll()
				.stream()
				.map(this::mapToDto)
				.collect(Collectors.toList());
	}

	@Override
	public ClientDto getById(Long id) {
		Client client = clientRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));
		return mapToDto(client);
	}

	@Override
	public ClientDto create(ClientDto clientDto) {
		Client client = mapToEntity(clientDto);
		client.setId(null);
		Client savedClient = clientRepository.save(client);
		return mapToDto(savedClient);
	}

	@Override
	public ClientDto update(ClientDto clientDto) {
		if (clientDto == null || clientDto.getId() == null) {
			return null;
		}
		Client existingClient = clientRepository.findById(clientDto.getId()).orElse(null);
		if (existingClient == null) {
			return null;
		}

		existingClient.setDni(clientDto.getDni());
		existingClient.setName(clientDto.getName());
		existingClient.setPassword(clientDto.getPassword());
		existingClient.setGender(clientDto.getGender());
		existingClient.setAge(clientDto.getAge());
		existingClient.setAddress(clientDto.getAddress());
		existingClient.setPhone(clientDto.getPhone());
		existingClient.setActive(clientDto.isActive());

		Client updatedClient = clientRepository.save(existingClient);
		return mapToDto(updatedClient);
	}

	@Override
	public ClientDto partialUpdate(Long id, PartialClientDto partialClientDto) {
		Client existingClient = clientRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));

		existingClient.setActive(partialClientDto.isActive());
		Client updatedClient = clientRepository.save(existingClient);
		return mapToDto(updatedClient);
	}

	@Override
	public void deleteById(Long id) {
		Client existingClient = clientRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));
		clientRepository.delete(existingClient);
	}

	private ClientDto mapToDto(Client client) {
		return new ClientDto(
				client.getId(),
				client.getDni(),
				client.getName(),
				client.getPassword(),
				client.getGender(),
				client.getAge(),
				client.getAddress(),
				client.getPhone(),
				client.isActive()
		);
	}

	private Client mapToEntity(ClientDto clientDto) {
		Client client = new Client();
		client.setId(clientDto.getId());
		client.setDni(clientDto.getDni());
		client.setName(clientDto.getName());
		client.setPassword(clientDto.getPassword());
		client.setGender(clientDto.getGender());
		client.setAge(clientDto.getAge());
		client.setAddress(clientDto.getAddress());
		client.setPhone(clientDto.getPhone());
		client.setActive(clientDto.isActive());
		return client;
	}
}
