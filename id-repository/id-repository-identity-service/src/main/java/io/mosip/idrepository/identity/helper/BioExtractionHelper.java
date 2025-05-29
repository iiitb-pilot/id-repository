package io.mosip.idrepository.identity.helper;

import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.TECHNICAL_ERROR;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.logger.spi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.exception.BiometricExtractionException;
import io.mosip.kernel.biometrics.constant.BiometricFunction;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.kernel.biosdk.provider.spi.iBioProviderApi;

/**
 * The Class BioExtractionHelper.
 * 
 *  @author Loganathan Sekar
 */
@Component
public class BioExtractionHelper {

	/** The mosip logger. */
	private Logger mosipLogger = IdRepoLogger.getLogger(BioExtractionHelper.class);

	/** The bio api factory. */
	@Autowired
	private BioAPIFactory bioApiFactory;
	
	/**
	 * Extract templates.
	 *
	 * @param birs the birs
	 * @param extractionFormats the extraction formats
	 * @return the list
	 * @throws BiometricExtractionException the biometric extraction exception
	 */
	public List<BIR> extractTemplates(List<BIR> birs, Map<String, String> extractionFormats) throws BiometricExtractionException {
		try {
			Map<BiometricType, List<BIR>> birsByType = birs.stream().collect(Collectors.groupingBy(bir -> bir.getBdbInfo().getType().get(0)));
			
			List<BIR> allExtractedTemplates =  new ArrayList<>();
			long startTime = System.currentTimeMillis();
			for (Entry<BiometricType,List<BIR>> entry : birsByType.entrySet()) {
				BiometricType modality = entry.getKey();
				mosipLogger.info("Entering extractTemplates - modality - " + modality.value());
				iBioProviderApi bioProvider = bioApiFactory.getBioProvider(BiometricType.fromValue(modality.value()),
						BiometricFunction.EXTRACT);
				mosipLogger.info("middle extractTemplates - modality - " + modality.value());
				List<BIR> extractedTemplates = bioProvider.extractTemplate(entry.getValue(), extractionFormats);
				mosipLogger.info("Time taken for extractTemplates " + modality + " for loop " + (System.currentTimeMillis() - startTime) + " ms");
				allExtractedTemplates.addAll(extractedTemplates);
			}
			return allExtractedTemplates;
			
		} catch (Exception e) {
			throw new BiometricExtractionException(TECHNICAL_ERROR, e);
		}
	}

}
