package com.devsu.hackerearth.backend.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.devsu.hackerearth.backend.client.controller.ClientController;
import com.devsu.hackerearth.backend.client.model.Client;
import com.devsu.hackerearth.backend.client.model.dto.ClientDto;
import com.devsu.hackerearth.backend.client.service.ClientService;

@SpringBootTest
public class sampleTest {

	private ClientService clientService = mock(ClientService.class);
	private ClientController clientController = new ClientController(clientService);

	@Autowired(required = false)
	private ClientService clientServiceIntegration;

	@Test
	void createClientTest() {
		// Arrange
		ClientDto newClient = new ClientDto(1L, "Dni", "Name", "Password", "Gender", 1, "Address", "9999999999", true);
		ClientDto createdClient = new ClientDto(1L, "Dni", "Name", "Password", "Gender", 1, "Address", "9999999999", true);
		when(clientService.create(newClient)).thenReturn(createdClient);

		// Act
		ResponseEntity<ClientDto> response = clientController.create(newClient);

		// Assert
		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertEquals(createdClient, response.getBody());
	}

	@Test
	void clientDomainEntityUnitTest() {
		// F5: Prueba unitaria para la entidad de dominio Client
		Client client = new Client();
		client.setId(10L);
		client.setName("Juan Perez");
		client.setDni("1234567890");
		client.setGender("Masculino");
		client.setAge(30);
		client.setAddress("Av. Amazonas 123");
		client.setPhone("0987654321");
		client.setPassword("secret123");
		client.setActive(true);

		assertEquals(10L, client.getId());
		assertEquals("Juan Perez", client.getName());
		assertEquals("1234567890", client.getDni());
		assertEquals("Masculino", client.getGender());
		assertEquals(30, client.getAge());
		assertEquals("Av. Amazonas 123", client.getAddress());
		assertEquals("0987654321", client.getPhone());
		assertEquals("secret123", client.getPassword());
		assertTrue(client.isActive());
	}

	@Test
	void clientIntegrationTest() {
		// F6: Prueba de integracion end-to-end con base de datos H2
		if (clientServiceIntegration != null) {
			ClientDto dto = new ClientDto(null, "1712345678", "Carlos Mendoza", "claveSegura1", "Masculino", 32, "Quito Centro", "0991234567", true);
			ClientDto created = clientServiceIntegration.create(dto);
			assertNotNull(created.getId());

			ClientDto fetched = clientServiceIntegration.getById(created.getId());
			assertEquals("Carlos Mendoza", fetched.getName());
			assertEquals("1712345678", fetched.getDni());
			assertTrue(fetched.isActive());
		}
	}
}
