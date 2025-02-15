package io.mosip.credential.request.generator.repositary;

import java.time.LocalDateTime;

import javax.persistence.LockModeType;
import javax.persistence.QueryHint;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;



// TODO: Auto-generated Javadoc
/**
 * The Interface CredentialRepositary.
 *
 * @author Sowmya
 * 
 *         The Interface CredentialRepositary.
 * @param <T> the generic type
 * @param <E> the element type
 */
@Repository
public interface CredentialRepositary<T extends CredentialEntity, E> extends BaseRepository<T, E> {



	@Query("SELECT crdn FROM CredentialEntity crdn WHERE crdn.statusCode= :statusCode")
	Page<CredentialEntity> findByStatusCode(@Param("statusCode") String statusCode, Pageable pageable);


	@Query("SELECT crdn FROM CredentialEntity crdn WHERE crdn.statusCode= :statusCode and crdn.updateDateTime>= :effectiveDTimes")
	Page<CredentialEntity> findByStatusCodeWithEffectiveDtimes(@Param("statusCode") String statusCode,
			@Param("effectiveDTimes") LocalDateTime effectiveDTimes,
			Pageable pageable);
	
	@Transactional
	@Lock(value = LockModeType.PESSIMISTIC_WRITE)
	@QueryHints({ @QueryHint(name = "javax.persistence.lock.timeout", value = "1") })
	@Query("select c from CredentialEntity c where c.statusCode=:statusCode")
	Page<CredentialEntity> findCredentialByStatusCode(@Param("statusCode")String statusCode, Pageable pageable);

	@Transactional
	@Query(value = "SELECT * FROM credential_transaction ct"
			+ " WHERE ct.batch_id=:batchId ORDER BY cr_dtimes FOR UPDATE SKIP LOCKED", nativeQuery = true)
	List<CredentialEntity> findCredentialByStatusCode(@Param("batchId")String batchId);

	@Modifying
	@Transactional
	@Query(value = "update credential_transaction ct set batch_id =:batchId where ct.id in (select ct1.id from credential_transaction ct1 "
			+ " WHERE ct1.status_code=:statusCode and ct1.batch_id is null ORDER BY ct1.cr_dtimes LIMIT :pageSize)", nativeQuery = true)
	int updateBatchIdByStatusCode(@Param("batchId")String batchId, @Param("statusCode")String statusCode, @Param("pageSize") int pageSize);

	/**
	 * Find credential by status codes.
	 *
	 * @param statusCodes the status codes
	 * @param type        the type
	 * @param pageable    the pageable
	 * @return the page
	 */
	@Transactional
	@Lock(value = LockModeType.PESSIMISTIC_WRITE)
	@QueryHints({ @QueryHint(name = "javax.persistence.lock.timeout", value = "1") })
	@Query("SELECT crdn FROM CredentialEntity crdn WHERE crdn.statusCode in :statusCodes")
	Page<CredentialEntity> findCredentialByStatusCodes(@Param("statusCodes") String[] statusCodes, Pageable pageable);

	@Transactional
	@Query(value = "SELECT * FROM credential_transaction ct"
			+ " WHERE ct.batch_id=:batchId ORDER BY cr_dtimes FOR UPDATE SKIP LOCKED", nativeQuery = true)
	List<CredentialEntity> findCredentialByStatusCodes(@Param("batchId")String batchId);


	@Modifying
	@Transactional
	@Query(value = "update credential_transaction ct set status_code = 'REPROCESS', batch_id =:batchId where ct.id in (select ct1.id from credential_transaction ct1 "
			+ " WHERE ct1.status_code in :statusCodes ORDER BY cr_dtimes LIMIT :pageSize)", nativeQuery = true)
	int updateBatchIdByStatusCodes(@Param("batchId")String batchId, @Param("statusCodes")String[] statusCodes, @Param("pageSize") int pageSize);

}
