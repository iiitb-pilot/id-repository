package io.mosip.idrepository.identity.helper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.builder.IdentityIssuanceProfileBuilder;
import io.mosip.idrepository.core.dto.DocumentsDTO;
import io.mosip.idrepository.core.dto.IdentityIssuanceProfile;
import io.mosip.idrepository.core.dto.IdentityMapping;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.identity.entity.AnonymousProfileEntity;
import io.mosip.idrepository.identity.repository.AnonymousProfileRepo;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.retry.WithRetry;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.UUIDUtils;

import static io.mosip.idrepository.core.constant.IdRepoConstants.SPLITTER;

@Component
@Transactional
public class AnonymousProfileHelper {
	
	Logger mosipLogger = IdRepoLogger.getLogger(AnonymousProfileHelper.class);

	@Autowired
	private AnonymousProfileRepo anonymousProfileRepo;

	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private ObjectStoreHelper objectStoreHelper;
	
	@Autowired
	private ChannelInfoHelper channelInfoHelper;

	@Value("${mosip.identity.mapping-file}")
	private String identityMappingJson;
	
	private byte[] oldUinData;

	private byte[] newUinData;
	
	private String regId;

	private String oldCbeff;

	private String newCbeff;
	
	private String uinHash;
	
	private String oldCbeffRefId;
	
	private String newCbeffRefId;
	
	@PostConstruct
	public void init() throws IOException {
		try (InputStream xsdBytes = new URL(identityMappingJson).openStream()) {
			IdentityMapping identityMapping = mapper.readValue(IOUtils.toString(xsdBytes, StandardCharsets.UTF_8),
					IdentityMapping.class);
			IdentityIssuanceProfileBuilder.setIdentityMapping(identityMapping);
		}
		IdentityIssuanceProfileBuilder.setDateFormat(EnvUtil.getIovDateFormat());
	}

	@Async("anonymousProfileExecutor")
	public void buildAndsaveProfile(boolean isDraft) {
		if (!isDraft)
			try {
				long startTime = System.currentTimeMillis();
				List<DocumentsDTO> oldDocList = List.of(new DocumentsDTO());
				List<DocumentsDTO> newDocList = List.of(new DocumentsDTO());
				if (Objects.isNull(oldCbeff) && Objects.nonNull(oldCbeffRefId))
					this.oldCbeff = CryptoUtil
							.encodeToURLSafeBase64(objectStoreHelper.getBiometricObject(uinHash, oldCbeffRefId));
				if (Objects.isNull(newCbeff) && Objects.nonNull(newCbeffRefId))
					this.newCbeff = CryptoUtil
							.encodeToURLSafeBase64(objectStoreHelper.getBiometricObject(uinHash, newCbeffRefId));
				if (Objects.nonNull(oldCbeff))
					oldDocList = List.of(new DocumentsDTO(IdentityIssuanceProfileBuilder.getIdentityMapping()
							.getIdentity().getIndividualBiometrics().getValue(), oldCbeff));
				if (Objects.nonNull(newCbeff))
					newDocList = List.of(new DocumentsDTO(IdentityIssuanceProfileBuilder.getIdentityMapping()
							.getIdentity().getIndividualBiometrics().getValue(), newCbeff));
				String id = UUID.randomUUID().toString();
//				String id = UUIDUtils.getUUID(UUIDUtils.NAMESPACE_OID, regId).toString();
				mosipLogger.debug(IdRepoSecurityManager.getUser(), "AnonymousProfileHelper", "buildAndsaveProfile",
						"Before Anonymous profile primary key " + id + " for regId " + regId);
				IdentityIssuanceProfile profile = IdentityIssuanceProfile.builder()
						.setFilterLanguage(EnvUtil.getAnonymousProfileFilterLanguage())
						.setProcessName(Objects.isNull(oldUinData) ? "New" : "Update").setOldIdentity(oldUinData)
						.setOldDocuments(oldDocList).setNewIdentity(newUinData).setNewDocuments(newDocList).build();
				mosipLogger.debug(IdRepoSecurityManager.getUser(), "AnonymousProfileHelper", "buildAndsaveProfile",
						"After Anonymous profile primary key " + id + " for regId " + regId);
				AnonymousProfileEntity anonymousProfile = AnonymousProfileEntity.builder().id(id)
						.profile(mapper.writeValueAsString(profile)).createdBy(IdRepoSecurityManager.getUser())
						.crDTimes(DateUtils.getUTCCurrentDateTime()).build();
				anonymousProfileRepo.save(anonymousProfile);
				updateChannelInfo();
				mosipLogger.debug(IdRepoSecurityManager.getUser(), "AnonymousProfileHelper", "buildAndsaveProfile",
						"Total time taken to complete anonymousProfileRepo save of " + regId + " (" + (System.currentTimeMillis() - startTime) + "ms)");
			} catch (Exception e) {
				mosipLogger.debug(IdRepoSecurityManager.getUser(), "AnonymousProfileHelper", "buildAndsaveProfile",
						regId + " (" + e.getMessage() + ")");
				mosipLogger.warn(IdRepoSecurityManager.getUser(), "AnonymousProfileHelper", "buildAndsaveProfile",
						ExceptionUtils.getStackTrace(e));
			}
	}

	@WithRetry
	public void updateChannelInfo() {
		channelInfoHelper.updatePhoneChannelInfo(oldUinData, newUinData);
		channelInfoHelper.updateEmailChannelInfo(oldUinData, newUinData);
	}

	public AnonymousProfileHelper setOldUinData(byte[] oldUinData) {
		this.oldUinData = oldUinData;
		return this;
	}

	public AnonymousProfileHelper setNewUinData(byte[] newUinData) {
		this.newUinData = newUinData;
		return this;
	}

	public AnonymousProfileHelper setOldCbeff(String oldCbeff) {
		this.oldCbeff = oldCbeff;
		return this;
	}
	
	public boolean isOldCbeffPresent() {
		return Objects.nonNull(this.oldCbeff);
	} 

	public AnonymousProfileHelper setNewCbeff(String newCbeff) {
		this.newCbeff = newCbeff;
		return this;
	}

	public boolean isNewCbeffPresent() {
		return Objects.nonNull(this.newCbeff);
	}

	public AnonymousProfileHelper setOldCbeff(String uinHash, String fileRefId) {
		if (Objects.isNull(oldCbeff)) {
			String substringHash = StringUtils.substringAfter(uinHash, "_");
			this.uinHash = StringUtils.isBlank(substringHash) ? uinHash : substringHash;
			this.oldCbeffRefId = fileRefId;
		}
		return this;
	}

	public AnonymousProfileHelper setNewCbeff(String uinHash, String fileRefId) {
		if (Objects.isNull(newCbeff)) {
			String substringHash = StringUtils.substringAfter(uinHash, "_");
			this.uinHash = StringUtils.isBlank(substringHash) ? uinHash : substringHash;
			this.newCbeffRefId = fileRefId;
		}
		return this;
	}

	public AnonymousProfileHelper setRegId(String regId) {
		if (Objects.nonNull(this.regId) && !this.regId.contentEquals(regId))
			resetData();
		this.regId = regId;
		return this;
	}

	private void resetData() {
		this.oldUinData = null;
		this.newUinData = null;
		this.oldCbeff = null;
		this.newCbeff = null;
		this.uinHash = null;
		this.newCbeffRefId = null;
		this.oldCbeffRefId = null;
		this.regId = null;
	}

}