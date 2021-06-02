package org.sormas.e2etests.steps.web.application.persons;

import static org.sormas.e2etests.pages.application.events.EventDirectoryPage.getByEventUuid;
import static org.sormas.e2etests.pages.application.persons.EditPersonPage.*;
import static org.sormas.e2etests.pages.application.persons.PersonDirectoryPage.*;

import cucumber.api.java8.En;
import javax.inject.Inject;
import org.sormas.e2etests.helpers.WebDriverHelpers;
import org.sormas.e2etests.steps.web.application.events.EditEventSteps;

public class PersonDirectorySteps implements En {

  private final WebDriverHelpers webDriverHelpers;

  @Inject
  public PersonDirectorySteps(WebDriverHelpers webDriverHelpers) {
    this.webDriverHelpers = webDriverHelpers;

    When(
        "I search for specific person in person directory",
        () -> {
          webDriverHelpers.waitUntilElementIsVisibleAndClickable(SEARCH_PERSON_BY_FREE_TEXT);
          final String personUuid = EditEventSteps.person.getUuid();
          webDriverHelpers.fillAndSubmitInWebElement(SEARCH_PERSON_BY_FREE_TEXT, personUuid);
        });

    When(
        "I click on specific person in person directory",
        () -> {
          final String personUuid = EditEventSteps.person.getUuid();
          webDriverHelpers.clickOnWebElementBySelector(getByPersonUuid(personUuid));
        });

    When(
        "I check if event is available at person information",
        () -> {
          webDriverHelpers.waitUntilElementIsVisibleAndClickable(EDIT_PERSON_SEE_EVENTS_FOR_PERSON);
          webDriverHelpers.clickOnWebElementBySelector(EDIT_PERSON_SEE_EVENTS_FOR_PERSON);
          final String eventUuid = EditEventSteps.event.getUuid();
          webDriverHelpers.waitUntilElementIsVisibleAndClickable(getByEventUuid(eventUuid));
        });
  }
}
