package com.devsu.hackerearth.backend.account.client;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ClientServiceClient {

	private final RestTemplate restTemplate;

	@Value("${client.service.url:http://localhost:8001/api/clients}")
	private String clientServiceUrl;

	public ClientServiceClient(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	public String getClientName(Long clientId) {
		try {
			String url = clientServiceUrl + "/" + clientId;
			Map<?, ?> response = restTemplate.getForObject(url, Map.class);
			if (response != null && response.containsKey("name")) {
				return (String) response.get("name");
			}
		} catch (Exception e) {
			// Fallback en caso de fallo de conexion o en ejecucion aislada
		}
		return "Cliente " + clientId;
	}
}
