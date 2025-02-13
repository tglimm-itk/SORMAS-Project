package de.symeda.sormas.backend.task;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import de.symeda.sormas.api.contact.ContactDto;
import de.symeda.sormas.api.person.PersonDto;
import de.symeda.sormas.api.task.TaskContext;
import de.symeda.sormas.api.task.TaskDto;
import de.symeda.sormas.api.task.TaskStatus;
import de.symeda.sormas.api.task.TaskType;
import de.symeda.sormas.api.user.DefaultUserRole;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.backend.AbstractBeanTest;
import de.symeda.sormas.backend.TestDataCreator;
import de.symeda.sormas.backend.caze.Case;
import de.symeda.sormas.backend.common.TaskCreationException;
import de.symeda.sormas.backend.contact.Contact;
import de.symeda.sormas.backend.infrastructure.district.District;
import de.symeda.sormas.backend.infrastructure.region.Region;
import de.symeda.sormas.backend.location.Location;
import de.symeda.sormas.backend.person.Person;
import de.symeda.sormas.backend.user.User;
import de.symeda.sormas.backend.user.UserService;

public class TaskServiceTest extends AbstractBeanTest {

	@InjectMocks
	private TaskService taskService;

	@Mock
	private UserService userService;

	@Test
	public void testGetAllAfter() {

		TestDataCreator.RDCF rdcf = creator.createRDCF();
		UserDto user = creator.createUser(rdcf, "U", "Ser", creator.getUserRoleReference(DefaultUserRole.SURVEILLANCE_SUPERVISOR));
		loginWith(user);

		List<Task> result;

		// 0. Without tasks
		assertThat(getTaskService().getAllAfter(null), is(empty()));

		// 1. One task
		TaskDto task1 = creator.createTask(user.toReference());
		result = getTaskService().getAllAfter(null);
		assertThat(result, hasSize(1));
		assertThat(getTaskService().getAllAfter(result.get(0).getChangeDate()), is(empty()));

		// 2. Two tasks
		TaskDto task2 = creator.createTask(user.toReference());
		result = getTaskService().getAllAfter(null);
		assertThat(result.stream().map(e -> e.getUuid()).collect(Collectors.toList()), contains(task1.getUuid(), task2.getUuid()));
		assertThat(getTaskService().getAllAfter(result.get(0).getChangeDate()), hasSize(1));
		assertThat(getTaskService().getAllAfter(result.get(1).getChangeDate()), is(empty()));
	}

	@Test
	public void testGetTaskAssigneeFromContactWithContactOfficer() throws TaskCreationException {
		User contactOfficer = new User();
		contactOfficer.setId(1L);
		Contact contact = new Contact();
		contact.setContactOfficer(contactOfficer);

		User actualAssignee = taskService.getTaskAssignee(contact);
		assertEquals(actualAssignee.getId(), contactOfficer.getId());
	}

	@Test
	public void testGetTaskAssigneeFromDistrictOfficers() throws TaskCreationException {
		User contactOfficer = new User();
		contactOfficer.setId(1L);
		District district = new District();
		Contact contact = new Contact();
		contact.setDistrict(district);

		Mockito.when(userService.getRandomDistrictUser(any(District.class), eq(UserRight.CONTACT_RESPONSIBLE))).thenReturn(contactOfficer);

		User actualAssignee = taskService.getTaskAssignee(contact);
		assertEquals(actualAssignee.getId(), contactOfficer.getId());
	}

	@Test
	public void testGetTaskAssigneeFromRegionSupervisors() throws TaskCreationException {
		User contactSupervisor = new User();
		contactSupervisor.setId(1L);
		Contact contact = new Contact();
		Location location = new Location();
		Person person = new Person();
		person.setAddress(location);
		contact.setPerson(person);
		Region region = new Region();
		contact.setRegion(region);

		Mockito.when(userService.getRandomRegionUser(any(Region.class), eq(UserRight.CONTACT_RESPONSIBLE))).thenReturn(contactSupervisor);

		User actualAssignee = taskService.getTaskAssignee(contact);
		assertEquals(actualAssignee.getId(), contactSupervisor.getId());
	}

	@Test
	public void testGetTaskAssigneeException() throws TaskCreationException {
		Contact contact = new Contact();
		Location location = new Location();
		Person person = new Person();
		person.setAddress(location);
		contact.setPerson(person);
		Case caze = new Case();
		contact.setCaze(caze);

		Mockito.when(userService.getRandomRegionUser(any(Region.class), any())).thenReturn(null);

		assertThrows(TaskCreationException.class, () -> taskService.getTaskAssignee(contact));
	}

	@Test
	public void testFindBy() {
		/*
		 * setup of test environment:
		 * - entities named like other* usually refer to something that shall sometimes exclude tasks from the result
		 * - task naming pattern includes all related entities
		 */

		TestDataCreator.RDCF rdcf = creator.createRDCF();
		UserDto user = creator.createUser(rdcf, "U", "Ser", creator.getUserRoleReference(DefaultUserRole.SURVEILLANCE_SUPERVISOR));
		UserDto otherUser = creator.createUser(rdcf, "Other", "User", creator.getUserRoleReference(DefaultUserRole.SURVEILLANCE_SUPERVISOR));
		PersonDto person = creator.createPerson();
		ContactDto contact = creator.createContact(rdcf, otherUser.toReference(), person.toReference());
		ContactDto otherContact = creator.createContact(rdcf, otherUser.toReference(), person.toReference());

		// find only by assignee
		TaskDto taskUser = creator.createTask(user.toReference());

		List<Task> result = getTaskService().findByAssigneeContactTypeAndStatuses(user.toReference(), null, null, null);
		assertEquals(1, result.size());
		assertEquals(taskUser.getUuid(), result.get(0).getUuid());

		result = getTaskService().findByAssigneeContactTypeAndStatuses(user.toReference(), contact.toReference(), null, null);
		assertTrue(result.isEmpty());

		result = getTaskService().findByAssigneeContactTypeAndStatuses(user.toReference(), null, TaskType.CONTACT_FOLLOW_UP, null);
		assertTrue(result.isEmpty());

		result = getTaskService().findByAssigneeContactTypeAndStatuses(user.toReference(), null, null, TaskStatus.IN_PROGRESS);
		assertTrue(result.isEmpty());

		// find only by contact
		TaskDto taskContactOtherUser = creator.createTask(
			TaskContext.CONTACT,
			TaskType.CONTACT_FOLLOW_UP,
			TaskStatus.IN_PROGRESS,
			null,
			contact.toReference(),
			null,
			null,
			otherUser.toReference());

		result = getTaskService().findByAssigneeContactTypeAndStatuses(null, contact.toReference(), null, null);
		assertEquals(1, result.size());
		assertEquals(taskContactOtherUser.getUuid(), result.get(0).getUuid());

		// find only by type
		result = getTaskService().findByAssigneeContactTypeAndStatuses(null, null, TaskType.CONTACT_FOLLOW_UP, null);
		assertEquals(1, result.size());
		assertEquals(taskContactOtherUser.getUuid(), result.get(0).getUuid());

		// find only by statuses
		result = getTaskService().findByAssigneeContactTypeAndStatuses(null, null, null, TaskStatus.IN_PROGRESS);
		assertEquals(1, result.size());
		assertEquals(taskContactOtherUser.getUuid(), result.get(0).getUuid());

		// find by multiple statuses
		result = getTaskService().findByAssigneeContactTypeAndStatuses(null, null, null, TaskStatus.DONE, TaskStatus.IN_PROGRESS);
		assertEquals(1, result.size());
		assertEquals(taskContactOtherUser.getUuid(), result.get(0).getUuid());

		// find multiple
		result = getTaskService().findByAssigneeContactTypeAndStatuses(null, null, null, TaskStatus.PENDING, TaskStatus.IN_PROGRESS);
		assertEquals(2, result.size());
		List<String> resultUuidList = Arrays.asList(taskUser.getUuid(), taskContactOtherUser.getUuid());
		assertTrue(resultUuidList.contains(result.get(0).getUuid()));
		assertTrue(resultUuidList.contains(result.get(1).getUuid()));

		// find by multiple
		result = getTaskService()
			.findByAssigneeContactTypeAndStatuses(user.toReference(), null, TaskType.OTHER, TaskStatus.PENDING, TaskStatus.IN_PROGRESS);
		assertEquals(1, result.size());
		assertEquals(taskUser.getUuid(), result.get(0).getUuid());
	}
}
