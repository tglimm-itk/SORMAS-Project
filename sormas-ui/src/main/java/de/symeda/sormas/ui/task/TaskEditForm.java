package de.symeda.sormas.ui.task;

import java.util.List;

import com.vaadin.data.Property;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Field;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextArea;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.caze.CaseReferenceDto;
import de.symeda.sormas.api.contact.ContactReferenceDto;
import de.symeda.sormas.api.task.TaskContext;
import de.symeda.sormas.api.task.TaskDto;
import de.symeda.sormas.api.task.TaskType;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.api.user.UserReferenceDto;
import de.symeda.sormas.api.user.UserRole;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.login.LoginHelper;
import de.symeda.sormas.ui.utils.AbstractEditForm;
import de.symeda.sormas.ui.utils.DateTimeField;
import de.symeda.sormas.ui.utils.FieldHelper;
import de.symeda.sormas.ui.utils.LayoutUtil;

@SuppressWarnings("serial")
public class TaskEditForm extends AbstractEditForm<TaskDto> {
	
    private static final String HTML_LAYOUT = 
    		LayoutUtil.fluidRow(LayoutUtil.loc(TaskDto.TASK_CONTEXT), 
    				LayoutUtil.locs(TaskDto.CAZE, TaskDto.EVENT, TaskDto.CONTACT))+
			LayoutUtil.fluidRowLocs(TaskDto.TASK_TYPE)+
			LayoutUtil.fluidRowLocs(TaskDto.SUGGESTED_START, TaskDto.DUE_DATE)+
			LayoutUtil.fluidRowLocs(TaskDto.ASSIGNEE_USER, TaskDto.PRIORITY)+
			LayoutUtil.fluidRowLocs(TaskDto.CREATOR_COMMENT)+
			LayoutUtil.fluidRowLocs(TaskDto.ASSIGNEE_REPLY)+
			LayoutUtil.fluidRowLocs(TaskDto.TASK_STATUS)
			;

    public TaskEditForm() {
        super(TaskDto.class, TaskDto.I18N_PREFIX);
        addValueChangeListener(e -> {
    		updateByTaskContext();
    		updateByCreatingAndAssignee();
        });
    }
    
	@Override
	protected void addFields() {

    	addField(TaskDto.CAZE, ComboBox.class);
    	addField(TaskDto.EVENT, ComboBox.class);
    	addField(TaskDto.CONTACT, ComboBox.class);
    	addField(TaskDto.SUGGESTED_START, DateTimeField.class);
    	addField(TaskDto.DUE_DATE, DateTimeField.class);
    	addField(TaskDto.PRIORITY, ComboBox.class);
    	addField(TaskDto.TASK_STATUS, OptionGroup.class);

    	OptionGroup taskContext = addField(TaskDto.TASK_CONTEXT, OptionGroup.class);
    	taskContext.setImmediate(true);
    	taskContext.addValueChangeListener(new Property.ValueChangeListener() {
			@Override
			public void valueChange(Property.ValueChangeEvent event) {
				updateByTaskContext();
			}
		});
    	    	
    	ComboBox taskType = addField(TaskDto.TASK_TYPE, ComboBox.class);
    	taskType.setItemCaptionMode(ItemCaptionMode.ID_TOSTRING);
    	taskType.setImmediate(true);
    	taskType.addValueChangeListener(new Property.ValueChangeListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void valueChange(Property.ValueChangeEvent event) {
				TaskType taskType = (TaskType)event.getProperty().getValue();
				if (taskType != null) {
					((Field<TaskContext>)getFieldGroup().getField(TaskDto.TASK_CONTEXT)).setValue(taskType.getTaskContext());
				}
			}
		});
    	
    	ComboBox assigneeUser = addField(TaskDto.ASSIGNEE_USER, ComboBox.class);
    	assigneeUser.addValueChangeListener(e -> updateByCreatingAndAssignee());
    	assigneeUser.setImmediate(true);
    	List<UserReferenceDto> users = FacadeProvider.getUserFacade().getAssignableUsers(LoginHelper.getCurrentUserAsReference());
    	TaskController taskController = ControllerProvider.getTaskController();
    	for (UserReferenceDto user : users) {
        	assigneeUser.addItem(user);
    		assigneeUser.setItemCaption(user, taskController.getUserCaptionWithPendingTaskCount(user));
    	}

    	addField(TaskDto.CREATOR_COMMENT, TextArea.class).setRows(2);
    	addField(TaskDto.ASSIGNEE_REPLY, TextArea.class).setRows(2);
    	
    	setRequired(true, TaskDto.TASK_CONTEXT, TaskDto.TASK_TYPE, TaskDto.ASSIGNEE_USER, TaskDto.DUE_DATE);
    }

	private void updateByCreatingAndAssignee() {
		
		TaskDto value = getValue();
		if (value != null) {
			boolean creating = value.getCreationDate() == null;
	
			UserDto user = LoginHelper.getCurrentUser();
			boolean creator = user.equals(value.getCreatorUser());
			boolean supervisor = UserRole.isSupervisor(user.getUserRoles());
			boolean assignee = user.equals(getFieldGroup().getField(TaskDto.ASSIGNEE_USER).getValue());
			
			setVisible(!creating || assignee, TaskDto.ASSIGNEE_REPLY, TaskDto.TASK_STATUS);
			if (creating && !assignee) {
				discard(TaskDto.ASSIGNEE_REPLY, TaskDto.TASK_STATUS);
			}
			
			setReadOnly(!(assignee || creator), TaskDto.TASK_STATUS);
			setReadOnly(!assignee, TaskDto.ASSIGNEE_REPLY);
			setReadOnly(!creator, TaskDto.CAZE, TaskDto.EVENT, TaskDto.CONTACT, 
					TaskDto.TASK_CONTEXT, TaskDto.TASK_TYPE, 
					TaskDto.PRIORITY, TaskDto.SUGGESTED_START, TaskDto.DUE_DATE,
					TaskDto.ASSIGNEE_USER, TaskDto.CREATOR_COMMENT);
			setReadOnly(!(creator || supervisor), 
					TaskDto.PRIORITY, TaskDto.SUGGESTED_START, TaskDto.DUE_DATE,
					TaskDto.ASSIGNEE_USER, TaskDto.CREATOR_COMMENT);
		}
	}

	private void updateByTaskContext() {
		
		TaskContext taskContext = (TaskContext)getFieldGroup().getField(TaskDto.TASK_CONTEXT).getValue();
		
		// Task types depending on task context
		ComboBox taskType = (ComboBox) getFieldGroup().getField(TaskDto.TASK_TYPE);
		FieldHelper.updateItems(taskType, TaskType.getTaskTypes(taskContext));
		
		// context reference depending on task context
		ComboBox caseField = (ComboBox) getFieldGroup().getField(TaskDto.CAZE);
		ComboBox eventField = (ComboBox) getFieldGroup().getField(TaskDto.EVENT);
		ComboBox contactField = (ComboBox) getFieldGroup().getField(TaskDto.CONTACT);
		if (taskContext != null) {
			switch (taskContext) {
			case CASE:
				FieldHelper.setFirstVisibleClearOthers(caseField, eventField, contactField);
				FieldHelper.setFirstRequired(caseField, eventField, contactField);
				List<CaseReferenceDto> cases = FacadeProvider.getCaseFacade().getSelectableCases(LoginHelper.getCurrentUserAsReference());
				FieldHelper.updateItems(caseField, cases);
				break;
			case EVENT:
				FieldHelper.setFirstVisibleClearOthers(eventField, caseField, contactField);
				FieldHelper.setFirstRequired(eventField, caseField, contactField);
				FieldHelper.updateItems(eventField, null);
				break;
			case CONTACT:
				FieldHelper.setFirstVisibleClearOthers(contactField, caseField, eventField);
				FieldHelper.setFirstRequired(contactField, caseField, eventField);
				List<ContactReferenceDto> contacts = FacadeProvider.getContactFacade().getSelectableContacts(LoginHelper.getCurrentUserAsReference());
				FieldHelper.updateItems(contactField, contacts);
				break;
			}
		}
		else {
			FieldHelper.setFirstVisibleClearOthers(null, caseField, eventField, contactField);
			FieldHelper.setFirstRequired(null, caseField, eventField, contactField);
		}
	}
	
	@Override
	protected String createHtmlLayout() {
		 return HTML_LAYOUT;
	}
}
