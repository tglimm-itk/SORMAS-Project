/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2020 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.symeda.sormas.api.sormastosormas;

import java.util.List;

import javax.ejb.Remote;

import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.sormastosormas.entities.DuplicateResult;
import de.symeda.sormas.api.sormastosormas.share.incoming.ShareRequestDataType;
import de.symeda.sormas.api.sormastosormas.validation.SormasToSormasValidationException;

@Remote
public interface SormasToSormasFacade {

	String getOrganizationId();

	List<SormasServerDescriptor> getAllAvailableServers();

	SormasServerDescriptor getSormasServerDescriptorById(String id);

	void rejectRequest(ShareRequestDataType dataType, String uuid, String comment) throws SormasToSormasException;

	void requestRejected(SormasToSormasEncryptedDataDto encryptedRejectData) throws SormasToSormasException;

	DuplicateResult acceptShareRequest(ShareRequestDataType dataType, String uuid, boolean checkDuplicates)
		throws SormasToSormasException, SormasToSormasValidationException;

	void revokeShare(String shareInfoUuid) throws SormasToSormasException;

	void revokeShareRequest(String requestUuid) throws SormasToSormasException;

	void requestsRevoked(SormasToSormasEncryptedDataDto encryptedRequestUuid) throws SormasToSormasException;

	void requestAccepted(SormasToSormasEncryptedDataDto encryptedAcceptData) throws SormasToSormasException;

	boolean isShareEnabledForUser();

	boolean isProcessingShareEnabledForUser();
	boolean isFeatureConfigured();

	boolean isAnyFeatureConfigured(FeatureType... sormasToSormasFeatures);

	boolean isSharingCasesEnabledForUser();

	boolean isSharingContactsEnabledForUser();

	boolean isSharingEventsEnabledForUser();

	boolean isSharingExternalMessagesEnabledForUser();
}
