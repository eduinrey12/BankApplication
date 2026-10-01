package com.devsu.hackerearth.backend.account.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.devsu.hackerearth.backend.account.model.Transaction;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

	List<Transaction> findByAccountId(Long accountId);

	List<Transaction> findByAccountIdAndDateBetweenOrderByIdAsc(Long accountId, Date dateStart, Date dateEnd);

	Optional<Transaction> findTopByAccountIdOrderByIdDesc(Long accountId);

	@Query("SELECT t FROM Transaction t WHERE t.accountId IN :accountIds AND t.date BETWEEN :dateStart AND :dateEnd ORDER BY t.id ASC")
	List<Transaction> findByAccountIdInAndDateBetween(
			@Param("accountIds") List<Long> accountIds,
			@Param("dateStart") Date dateStart,
			@Param("dateEnd") Date dateEnd
	);
}
