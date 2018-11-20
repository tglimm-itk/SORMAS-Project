/*******************************************************************************
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2018 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *******************************************************************************/
package de.symeda.sormas.ui.contact;

import java.util.Arrays;

import org.joda.time.LocalDate;

import com.vaadin.data.Validator;
import com.vaadin.data.validator.DateRangeValidator;
import com.vaadin.shared.ui.datefield.Resolution;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.DateField;
import com.vaadin.ui.Field;
import com.vaadin.ui.Link;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.I18nProperties;
import de.symeda.sormas.api.caze.CaseDataDto;
import de.symeda.sormas.api.caze.CaseReferenceDto;
import de.symeda.sormas.api.contact.ContactClassification;
import de.symeda.sormas.api.contact.ContactDto;
import de.symeda.sormas.api.contact.FollowUpStatus;
import de.symeda.sormas.api.person.PersonReferenceDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.user.UserRole;
import de.symeda.sormas.api.utils.Diseases.DiseasesConfiguration;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.login.LoginHelper;
import de.symeda.sormas.ui.utils.AbstractEditForm;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.FieldHelper;
import de.symeda.sormas.ui.utils.LayoutUtil;

@SuppressWarnings("serial")
public class ContactDataForm extends AbstractEditForm<ContactDto> {
	
	private static final String TO_CASE_BTN_LOC = "toCaseBtnLoc";
	private static final String CANCEL_OR_RESUME_FOLLOW_UP_BTN_LOC = "cancelOrResumeFollowUpBtnLoc";
	private static final String LOST_FOLLOW_UP_BTN_LOC = "lostFollowUpBtnLoc";
	
    private static final String HTML_LAYOUT = 
    		LayoutUtil.h3("Contact data")+
			LayoutUtil.fluidRowLocs(ContactDto.CONTACT_CLASSIFICATION, ContactDto.CONTACT_STATUS) +
			LayoutUtil.locCss(CssStyles.VSPACE_3, TO_CASE_BTN_LOC) +
			LayoutUtil.fluidRowLocs(ContactDto.LAST_CONTACT_DATE, ContactDto.UUID) +
			LayoutUtil.fluidRowLocs(ContactDto.REPORTING_USER, ContactDto.REPORT_DATE_TIME) +
			LayoutUtil.fluidRowLocs(ContactDto.CONTACT_PROXIMITY, "") +
			LayoutUtil.fluidRowLocs(ContactDto.RELATION_TO_CASE) +
			LayoutUtil.fluidRowLocs(ContactDto.DESCRIPTION) +
			LayoutUtil.h3("Follow-up status") +
			LayoutUtil.fluidRowLocs(ContactDto.FOLLOW_UP_STATUS, CANCEL_OR_RESUME_FOLLOW_UP_BTN_LOC, LOST_FOLLOW_UP_BTN_LOC) +
			LayoutUtil.fluidRowLocs(ContactDto.FOLLOW_UP_COMMENT) +
			LayoutUtil.fluidRowLocs(ContactDto.FOLLOW_UP_UNTIL, ContactDto.CONTACT_OFFICER)
		    ;

    public ContactDataForm(UserRight editOrCreateUserRight) {
        super(ContactDto.class, ContactDto.I18N_PREFIX, editOrCreateUserRight);
    }

    @Override
	protected void addFields() {
    	addField(ContactDto.CONTACT_CLASSIFICATION, OptionGroup.class);
    	addField(ContactDto.CONTACT_STATUS, OptionGroup.class);
    	addField(ContactDto.UUID, TextField.class);
    	addField(ContactDto.REPORTING_USER, ComboBox.class);
    	DateField lastContactDate = addField(ContactDto.LAST_CONTACT_DATE, DateField.class);
    	addField(ContactDto.REPORT_DATE_TIME, DateField.class);
    	OptionGroup contactProximity = addField(ContactDto.CONTACT_PROXIMITY, OptionGroup.class);
    	contactProximity.removeStyleName(ValoTheme.OPTIONGROUP_HORIZONTAL);
    	ComboBox relationToCase = addField(ContactDto.RELATION_TO_CASE, ComboBox.class);
    	addField(ContactDto.DESCRIPTION, TextArea.class).setRows(3);

    	addField(ContactDto.FOLLOW_UP_STATUS, ComboBox.class);
    	addField(ContactDto.FOLLOW_UP_COMMENT, TextArea.class).setRows(1);
    	addField(ContactDto.FOLLOW_UP_UNTIL, DateField.class);

    	ComboBox contactOfficerField = addField(ContactDto.CONTACT_OFFICER, ComboBox.class);
    	contactOfficerField.setNullSelectionAllowed(true);
    	
    	setReadOnly(true, ContactDto.UUID, ContactDto.REPORTING_USER, ContactDto.REPORT_DATE_TIME, 
    			ContactDto.CONTACT_STATUS, ContactDto.FOLLOW_UP_STATUS, ContactDto.FOLLOW_UP_UNTIL);
    	
    	FieldHelper.setRequiredWhen(getFieldGroup(), ContactDto.FOLLOW_UP_STATUS, 
    			Arrays.asList(ContactDto.FOLLOW_UP_COMMENT), 
    			Arrays.asList(FollowUpStatus.CANCELED, FollowUpStatus.LOST));

    	addValueChangeListener(e -> {
        	if (getValue() != null) {
	    		CaseDataDto caseDto = FacadeProvider.getCaseFacade().getCaseDataByUuid(getValue().getCaze().getUuid());
	    		updateLastContactDateValidator();
	    		updateDiseaseConfiguration(caseDto.getDisease());
	    		
	    		updateFollowUpStatusComponents();
    		
    	    	contactOfficerField.addItems(FacadeProvider.getUserFacade().getAssignableUsersByDistrict(caseDto.getDistrict(), false, UserRole.CONTACT_OFFICER));
    	    	
    	    	getContent().removeComponent(TO_CASE_BTN_LOC);
    	    	if (getValue().getResultingCase() != null) {
    	    		// link to case
    		    	Link linkToData = ControllerProvider.getCaseController().createLinkToData(getValue().getResultingCase().getUuid(), 
    		    			"Open case of this contact person");
    		    	getContent().addComponent(linkToData, TO_CASE_BTN_LOC);
    	    	}
    	    	else if (getValue().getContactClassification() == ContactClassification.CONFIRMED) {
    	    		// only when confirmed
    	    		if (LoginHelper.hasUserRight(UserRight.CONTACT_CONVERT)) {
	    		    	Button toCaseButton = new Button("Create a case for this contact person");
	    				toCaseButton.addStyleName(ValoTheme.BUTTON_LINK);
	    				
	    				toCaseButton.addClickListener(new ClickListener() {
	    					@Override
	    					public void buttonClick(ClickEvent event) {
	    						PersonReferenceDto personRef = getValue().getPerson();
	    						CaseReferenceDto caseRef = getValue().getCaze();
	    						CaseDataDto caze = FacadeProvider.getCaseFacade().getCaseDataByUuid(caseRef.getUuid());
	    						ControllerProvider.getCaseController().create(personRef, caze.getDisease(), getValue());
	    					}
	    				});
	    				
	    				getContent().addComponent(toCaseButton, TO_CASE_BTN_LOC);
    	    		}
    	    	}
        	}
    	});
    	
    	setRequired(true, ContactDto.CONTACT_CLASSIFICATION, ContactDto.CONTACT_STATUS);
    	FieldHelper.addSoftRequiredStyle(lastContactDate, contactProximity, relationToCase);
	}
    
    @SuppressWarnings("unchecked")
	private void updateFollowUpStatusComponents() {

		getContent().removeComponent(CANCEL_OR_RESUME_FOLLOW_UP_BTN_LOC);
		getContent().removeComponent(LOST_FOLLOW_UP_BTN_LOC);

		Field<FollowUpStatus> statusField = (Field<FollowUpStatus>) getField(ContactDto.FOLLOW_UP_STATUS);
		boolean followUpVisible = getValue() != null && statusField.isVisible();
		if (followUpVisible && LoginHelper.hasUserRight(UserRight.CONTACT_EDIT)) {
			FollowUpStatus followUpStatus = statusField.getValue();
			if (followUpStatus == FollowUpStatus.FOLLOW_UP) {
				
		    	Button cancelButton = new Button(I18nProperties.getFragment("Contact.cancelFollowUp"));
		    	cancelButton.setWidth(100, Unit.PERCENTAGE);
		    	cancelButton.addClickListener(new ClickListener() {
					@Override
					public void buttonClick(ClickEvent event) {
						Field<FollowUpStatus> statusField = (Field<FollowUpStatus>) getField(ContactDto.FOLLOW_UP_STATUS);
						statusField.setReadOnly(false);
						statusField.setValue(FollowUpStatus.CANCELED);
						statusField.setReadOnly(true);
						updateFollowUpStatusComponents();
					}
				});
				getContent().addComponent(cancelButton, CANCEL_OR_RESUME_FOLLOW_UP_BTN_LOC);

		    	Button lostButton = new Button(I18nProperties.getFragment("Contact.lostToFollowUp"));
		    	lostButton.setWidth(100, Unit.PERCENTAGE);
		    	lostButton.addClickListener(new ClickListener() {
					@Override
					public void buttonClick(ClickEvent event) {
						Field<FollowUpStatus> statusField = (Field<FollowUpStatus>) getField(ContactDto.FOLLOW_UP_STATUS);
						statusField.setReadOnly(false);
						statusField.setValue(FollowUpStatus.LOST);
						statusField.setReadOnly(true);
						updateFollowUpStatusComponents();
					}
				});
				getContent().addComponent(lostButton, LOST_FOLLOW_UP_BTN_LOC);
				
			} else if (followUpStatus == FollowUpStatus.CANCELED
					|| followUpStatus == FollowUpStatus.LOST) {

		    	Button resumeButton = new Button(I18nProperties.getFragment("Contact.resumeFollowUp"));
		    	resumeButton.addStyleName(CssStyles.FORCE_CAPTION);
		    	resumeButton.setWidth(100, Unit.PERCENTAGE);
		    	resumeButton.addClickListener(new ClickListener() {
					@Override
					public void buttonClick(ClickEvent event) {
						Field<FollowUpStatus> statusField = (Field<FollowUpStatus>) getField(ContactDto.FOLLOW_UP_STATUS);
						statusField.setReadOnly(false);
						statusField.setValue(FollowUpStatus.FOLLOW_UP);
						statusField.setReadOnly(true);
						updateFollowUpStatusComponents();
					}
				});
				getContent().addComponent(resumeButton, CANCEL_OR_RESUME_FOLLOW_UP_BTN_LOC);
			}
		}		
	}

	protected void updateLastContactDateValidator() {
    	Field<?> dateField = getField(ContactDto.LAST_CONTACT_DATE);
    	for (Validator validator : dateField.getValidators()) {
    		if (validator instanceof DateRangeValidator) {
    			dateField.removeValidator(validator);
    		}
    	}
    	if (getValue() != null) {
	    	dateField.addValidator(new DateRangeValidator(I18nProperties.getValidationError("beforeDate", dateField.getCaption(), getField(ContactDto.REPORT_DATE_TIME).getCaption()),
	    			null, new LocalDate(getValue().getReportDateTime()).plusDays(1).toDate(), Resolution.SECOND));
    	}
    }

	private void updateDiseaseConfiguration(Disease disease) {
		for (Object propertyId : getFieldGroup().getBoundPropertyIds()) {
			boolean visible = DiseasesConfiguration.isDefinedOrMissing(ContactDto.class, (String)propertyId, disease);
			getFieldGroup().getField(propertyId).setVisible(visible);
		}
	}

	@Override
	protected String createHtmlLayout() {
		 return HTML_LAYOUT;
	}
}
