@UI @Sanity @Event @EventImportExport
Feature: Event import and export tests

@issue=SORQA-131 @env_main
Scenario: Basic event export
Given I log in as a Admin User
  Then I click on the Events button from navbar
  And I click on the NEW EVENT button
  Then I create a new event with specific data
  And I click on the Events button from navbar
  Then I search for specific event in event directory
  When I click on the Export Event button
  Then I click on the Basic Event Export button
  And I check if downloaded data generated by basic event export option is correct

  @issue=SORQA-130 @env_main
  Scenario: Detailed event export
    Given I log in as a Admin User
    Then I click on the Events button from navbar
    And I click on the NEW EVENT button
    Then I create a new event with specific data
    And I click on the Events button from navbar
    Then I search for specific event in event directory
    When I click on the Export Event button
    Then I click on the Detailed Event Export button
    And I check if downloaded data generated by detailed event export option is correct

  @issue=SORQA-8047 @env_main
  Scenario: Basic event export with action date check
    Given I log in as a National User
    Then I click on the Events button from navbar
    When I click on the Actions button from Events view switcher
    When I click on the Export Event button
    Then I click on the Basic Event Export button
    Then I check if downloaded data generated by basic event action export option contains actionData

  @issue=SORQA-8047 @env_main
  Scenario: Detailed event export with action date check
    Given I log in as a National User
    Then I click on the Events button from navbar
    When I click on the Actions button from Events view switcher
    When I click on the Export Event button
    Then I click on the Detailed Event Export button
    Then I check if downloaded data generated by detailed event action export option contains actionData