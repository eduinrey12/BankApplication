package com.devsu.hackerearth.backend.account.controller;

import java.util.Date;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devsu.hackerearth.backend.account.model.dto.BankStatementDto;
import com.devsu.hackerearth.backend.account.model.dto.TransactionDto;
import com.devsu.hackerearth.backend.account.service.TransactionService;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    
	private final TransactionService transactionService;

	public TransactionController(TransactionService transactionService) {
		this.transactionService = transactionService;
	}

	@GetMapping
	public ResponseEntity<List<TransactionDto>> getAll(){
		return ResponseEntity.ok(transactionService.getAll());
	}

	@GetMapping("/{id}")
	public ResponseEntity<TransactionDto> get(@PathVariable Long id){
		TransactionDto transaction = transactionService.getById(id);
		if (transaction == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(transaction);
	}

	@PostMapping
	public ResponseEntity<TransactionDto> create(@RequestBody TransactionDto transactionDto){
		return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.create(transactionDto));
	}

	@PutMapping("/{id}")
	public ResponseEntity<TransactionDto> update(@PathVariable Long id, @RequestBody TransactionDto transactionDto){
		if (transactionDto == null) {
			return ResponseEntity.notFound().build();
		}
		TransactionDto updated = transactionService.update(transactionDto);
		if (updated == null && transactionDto.getId() == null) {
			transactionDto.setId(id);
			updated = transactionService.update(transactionDto);
		}
		if (updated == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(updated);
	}

	@PatchMapping("/{id}")
	public ResponseEntity<TransactionDto> partialUpdate(@PathVariable Long id, @RequestBody TransactionDto transactionDto){
		TransactionDto updated = transactionService.partialUpdate(id, transactionDto);
		if (updated == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(updated);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id){
		TransactionDto existing = transactionService.getById(id);
		if (existing == null) {
			return ResponseEntity.notFound().build();
		}
		transactionService.deleteById(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/clients/{clientId}/report")
	public ResponseEntity<List<BankStatementDto>> report(
			@PathVariable Long clientId,
			@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateTransactionStart,
			@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateTransactionEnd) {
		return ResponseEntity.ok(transactionService.getAllByAccountClientIdAndDateBetween(clientId, dateTransactionStart, dateTransactionEnd));
	}
}
