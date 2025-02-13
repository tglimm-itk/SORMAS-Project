package de.symeda.sormas.backend.campaign;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Date;

import org.junit.jupiter.api.Test;

import de.symeda.sormas.api.campaign.CampaignDto;
import de.symeda.sormas.api.campaign.CampaignReferenceDto;
import de.symeda.sormas.api.campaign.diagram.CampaignDashboardElement;
import de.symeda.sormas.api.user.DefaultUserRole;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.api.utils.ValidationRuntimeException;
import de.symeda.sormas.backend.AbstractBeanTest;
import de.symeda.sormas.backend.TestDataCreator;

public class CampaignFacadeEjbTest extends AbstractBeanTest {

	public static final int ONE_DAY_IN_MILLIS = 24 * 60 * 60 * 1000;

	@Test
	public void testGetAllAfter() {

		final TestDataCreator.RDCF rdcf = creator.createRDCF("Region", "District", "Community", "Facility");
		final UserDto user = creator.createUser(rdcf, creator.getUserRoleReference(DefaultUserRole.SURVEILLANCE_SUPERVISOR));
		loginWith(user);

		// 0. no data
		assertThat(getCampaignFacade().getAllAfter(null), is(empty()));

		// 1. One campaign
		CampaignDto campaign1 = creator.createCampaign(user);
		campaign1.setStartDate(new Date(System.currentTimeMillis()));
		campaign1 = getCampaignFacade().save(campaign1);
		assertThat(getCampaignFacade().getAllAfter(null), contains(campaign1));

		// 2. Two campaigns
		CampaignDto campaign2 = creator.createCampaign(user);
		campaign2.setStartDate(new Date(System.currentTimeMillis()));
		campaign2 = getCampaignFacade().save(campaign2);
		assertThat(getCampaignFacade().getAllAfter(null), contains(campaign1, campaign2));
	}

	@Test
	public void testGetLastStartedCampaign() {

		final TestDataCreator.RDCF rdcf = creator.createRDCF("Region", "District", "Community", "Facility");
		final UserDto user = creator.createUser(rdcf, creator.getUserRoleReference(DefaultUserRole.SURVEILLANCE_SUPERVISOR));
		final CampaignDto campaign1 = creator.createCampaign(user);
		campaign1.setStartDate(new Date(System.currentTimeMillis() - 7 * ONE_DAY_IN_MILLIS)); // last week
		getCampaignFacade().save(campaign1);
		final CampaignDto campaign2 = creator.createCampaign(user);
		campaign2.setStartDate(new Date(System.currentTimeMillis() - ONE_DAY_IN_MILLIS)); // yesterday
		getCampaignFacade().save(campaign2);
		final CampaignDto campaign3 = creator.createCampaign(user);
		campaign3.setStartDate(new Date(System.currentTimeMillis() + ONE_DAY_IN_MILLIS)); // tomorrow
		getCampaignFacade().save(campaign3);

		CampaignReferenceDto lastStartedCampaign = getCampaignFacade().getLastStartedCampaign();
		assertEquals(campaign2.getUuid(), lastStartedCampaign.getUuid());
	}

	@Test
	public void testCampaignDashboardElementsValidation() {

		final TestDataCreator.RDCF rdcf = creator.createRDCF("Region", "District", "Community", "Facility");
		final UserDto user = creator.createUser(rdcf, creator.getUserRoleReference(DefaultUserRole.SURVEILLANCE_SUPERVISOR));

		final CampaignDto campaign = creator.createCampaign(user);
		final ArrayList<CampaignDashboardElement> campaignDashboardElements = new ArrayList<>();
		campaignDashboardElements.add(new CampaignDashboardElement("diagram1", "tab1", null, 1, 50, 50));
		campaignDashboardElements.add(new CampaignDashboardElement("diagram2", "tab1", null, 2, 50, 50));
		campaignDashboardElements.add(new CampaignDashboardElement("diagram3", "tab2", null, 3, 50, 50));
		campaign.setCampaignDashboardElements(campaignDashboardElements);

		getCampaignDiagramDefinitionFacade().save(creator.createCampaignDiagramDefinition("diagram1", "Diagram one"));
		getCampaignDiagramDefinitionFacade().save(creator.createCampaignDiagramDefinition("diagram2", "Diagram two"));
		getCampaignDiagramDefinitionFacade().save(creator.createCampaignDiagramDefinition("diagram3", "Diagram three"));

		getCampaignFacade().validate(campaign);

		campaign.getCampaignDashboardElements().get(0).setSubTabId("subTab1");
		assertThrowsWithMessage(
			ValidationRuntimeException.class,
			"Campaign dashboard elements subTabId of campaign CampaignName are missing!",
			() -> getCampaignFacade().validate(campaign));

		campaign.getCampaignDashboardElements().get(0).setSubTabId(null);
		campaign.getCampaignDashboardElements().get(1).setSubTabId("subTab2");
		assertThrowsWithMessage(
			ValidationRuntimeException.class,
			"Campaign dashboard elements subTabId of campaign CampaignName are missing!",
			() -> getCampaignFacade().validate(campaign));

		campaign.getCampaignDashboardElements().get(0).setSubTabId("subTab1");
		campaign.getCampaignDashboardElements().get(1).setSubTabId("subTab2");
		campaign.getCampaignDashboardElements().get(2).setSubTabId("subTab3");
		getCampaignFacade().validate(campaign);

		campaign.getCampaignDashboardElements().get(2).setSubTabId(null);
		getCampaignFacade().validate(campaign);

		final String nonExistingDiagramId = "nonExistingDiagramId";
		campaign.getCampaignDashboardElements().get(0).setDiagramId(nonExistingDiagramId);
		assertThrowsWithMessage(
			ValidationRuntimeException.class,
			"Diagram nonExistingDiagramId from campaign CampaignName does not exist!",
			() -> getCampaignFacade().validate(campaign));
	}
}
