package io.mosip.idrepository.identity.helper;

import static io.mosip.idrepository.core.constant.IdRepoConstants.BIO_DATA_REFID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.DEMO_DATA_REFID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.OBJECT_STORE_ACCOUNT_NAME;
import static io.mosip.idrepository.core.constant.IdRepoConstants.OBJECT_STORE_ADAPTER_NAME;
import static io.mosip.idrepository.core.constant.IdRepoConstants.OBJECT_STORE_BUCKET_NAME;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.FILE_NOT_FOUND;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.kernel.core.logger.spi.Logger;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.amazonaws.services.s3.model.AmazonS3Exception;

import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.fsadapter.exception.FSAdapterException;

/**
 * @author Manoj SP
 *
 */
@Component
public class ObjectStoreHelper {
	
	@Value("${" + BIO_DATA_REFID + "}")
	private String bioDataRefId;
	
	@Value("${" + DEMO_DATA_REFID + "}")
	private String demoDataRefId;

	/** The Constant SLASH. */
	private static final String SLASH = "/";

	/** The Constant BIOMETRICS. */
	private static final String BIOMETRICS = "Biometrics";

	/** The Constant DEMOGRAPHICS. */
	private static final String DEMOGRAPHICS = "Demographics";

	@Value("${" + OBJECT_STORE_ACCOUNT_NAME + "}")
	private String objectStoreAccountName;

	@Value("${" + OBJECT_STORE_BUCKET_NAME + "}")
	private String objectStoreBucketName;

	@Value("${" + OBJECT_STORE_ADAPTER_NAME + "}")
	private String objectStoreAdapterName;
	
	private ObjectStoreAdapter objectStore;

	/** The mosip logger. */
	private Logger mosipLogger = IdRepoLogger.getLogger(ObjectStoreHelper.class);

	@Autowired
	public void setObjectStore(ApplicationContext context) {
		this.objectStore = context.getBean(objectStoreAdapterName, ObjectStoreAdapter.class);
	}

	/** The security manager. */
	@Autowired
	private IdRepoSecurityManager securityManager;

	public boolean demographicObjectExists(String uinHash, String fileRefId) {
		return exists(uinHash, false, fileRefId);
	}

	public boolean biometricObjectExists(String uinHash, String fileRefId) {
		return exists(uinHash, true, fileRefId);
	}

	public void putDemographicObject(String uinHash, String fileRefId, byte[] data) throws IdRepoAppException {
		putObject(uinHash, false, fileRefId, data, demoDataRefId);
	}

	public void putBiometricObject(String uinHash, String fileRefId, byte[] data) throws IdRepoAppException {
		putObject(uinHash, true, fileRefId, data, bioDataRefId);
	}

	public byte[] getDemographicObject(String uinHash, String fileRefId) throws IdRepoAppException {
		if (!this.demographicObjectExists(uinHash, fileRefId)) {
			throw new IdRepoAppException(FILE_NOT_FOUND);
		}
		return getObject(uinHash, false, fileRefId, demoDataRefId);
	}

	public byte[] getBiometricObject(String uinHash, String fileRefId) throws IdRepoAppException {
		if (!this.biometricObjectExists(uinHash, fileRefId)) {
			throw new IdRepoAppException(FILE_NOT_FOUND);
		}
		return getObject(uinHash, true, fileRefId, bioDataRefId);
	}
	
	public void deleteBiometricObject(String uinHash, String fileRefId) {
		if (this.biometricObjectExists(uinHash, fileRefId)) {
			long startTime = System.currentTimeMillis();
			String objectName = uinHash + SLASH + BIOMETRICS + SLASH + fileRefId;
			objectStore.deleteObject(objectStoreAccountName, objectStoreBucketName, null, null, objectName);
			mosipLogger.debug("Time taken for deleteObject call " + (System.currentTimeMillis() - startTime) + " ms");
		}
	}

	private boolean exists(String uinHash, boolean isBio, String fileRefId) {
		long startTime = System.currentTimeMillis();
		String objectName = uinHash + SLASH + (isBio ? BIOMETRICS : DEMOGRAPHICS) + SLASH + fileRefId;
		boolean isExist = objectStore.exists(objectStoreAccountName, objectStoreBucketName, null, null, objectName);
		mosipLogger.debug("Time taken for exists call " + (System.currentTimeMillis() - startTime) + " ms");
		return isExist;
	}

	private void putObject(String uinHash, boolean isBio, String fileRefId, byte[] data, String refId)
			throws IdRepoAppException {
		try {
			String objectName = uinHash + SLASH + (isBio ? BIOMETRICS : DEMOGRAPHICS) + SLASH + fileRefId;
			long encryptStartTime = System.currentTimeMillis();
			InputStream encryptData = new ByteArrayInputStream(securityManager.encrypt(data, refId));
			mosipLogger.debug("Time taken for putObject call encryption " + (System.currentTimeMillis() - encryptStartTime) + " ms");
			long startTime = System.currentTimeMillis();
			objectStore.putObject(objectStoreAccountName, objectStoreBucketName, null, null, objectName,
					encryptData);
			mosipLogger.debug("Time taken for putObject call S3 " + (System.currentTimeMillis() - startTime) + " ms");
		} catch (AmazonS3Exception | FSAdapterException e) {
			throw new IdRepoAppException(FILE_STORAGE_ACCESS_ERROR, e);
		}
	}

	private byte[] getObject(String uinHash, boolean isBio, String fileRefId, String refId) throws IdRepoAppException {
		try {
			long startTime = System.currentTimeMillis();
			String objectName = uinHash + SLASH + (isBio ? BIOMETRICS : DEMOGRAPHICS) + SLASH + fileRefId;
			InputStream osObject =  objectStore.getObject(objectStoreAccountName, objectStoreBucketName, null, null, objectName);
			mosipLogger.debug("Time taken for getObject call " + (System.currentTimeMillis() - startTime) + " ms");
			return securityManager.decrypt(IOUtils.toByteArray(osObject), refId);
		} catch (AmazonS3Exception | FSAdapterException | IOException e) {
			throw new IdRepoAppException(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR);
		}
	}
}
