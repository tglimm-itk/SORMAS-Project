@UI @Sanity @Contact @ContactImportExport
Feature: Contact import and export tests

  @issue=SORDEV-10043 @env_main
  Scenario: Contact basic export test
    When API: I create a new person
    Then API: I check that POST call body is "OK"
    And API: I check that POST call status code is 200
    Then API: I create a new contact
    Then API: I check that POST call body is "OK"
    And API: I check that POST call status code is 200
    Given I log in as a Admin User
    And I click on the Contacts button from navbar
    Then I filter by Contact uuid
    And I click on the Export contact button
    Then I click on the Basic Contact Export button
    Then I check if downloaded data generated by basic contact option is correct

  @issue=SORDEV-10046 @env_main
  Scenario: Contact custom export test
    When API: I create a new person
    Then API: I check that POST call body is "OK"
    And API: I check that POST call status code is 200
    Then API: I create a new contact
    Then API: I check that POST call body is "OK"
    And API: I check that POST call status code is 200
    Given I log in as a Admin User
    And I click on the Contacts button from navbar
    Then I filter by Contact uuid
    And I click on the Export contact button
    Then I click on the Custom Contact Export button
    And I click on the New Export Configuration button in Custom Contact Export popup
    Then I fill Configuration Name field with "Test Configuration" in Custom Contact Export popup
    And I select specific data to export in Export Configuration for Custom Contact Export
    When I download created custom contact export file
    And I delete created custom contact export file
    Then I check if downloaded data generated by custom contact option is correct

  @issue=SORDEV-6134 @env_de
  Scenario: Test Re-add primary phone number and primary email address to import templates for Contact
    Given I log in as a Admin User
    Then I click on the Contacts button from navbar
    And I click on the More button on Contact directory page
    And I click on the Import button from Contact directory
    Then I select the "ImportContactPrio.csv" CSV file in the file picker
    And I click on the "DATENIMPORT STARTEN" button from the Import Contact popup
    Then I click to create new person from the Contact Import popup if Pick or create popup appears
    And I check that an import success notification appears in the Import Contact popup
    Then I close Import Contact form
    And I filter by "UpdateTestContact ImportPrio" as a Person's full name on Contact Directory Page
    And I click APPLY BUTTON in Contact Directory Page
    And I open the last created UI Contact
    Then I open Contact Person tab
    And I check that Type of Contact details with Primary telephone as an option is visible on Edit Contact Person Page
    And I check that Type of Contact details with Primary email address as an option is visible on Edit Contact Person Page

  @issue=SORDEV-5479 @env_main
  Scenario: Test for exporting and importing case contact
    When API: I create a new person
    Then API: I check that POST call body is "OK"
    And API: I check that POST call status code is 200
    Then API: I create a new case
    Then API: I check that POST call body is "OK"
    And API: I check that POST call status code is 200
    Given I log in as a Admin User
    And I click on the Cases button from navbar
    And I open the last created Case via API
    When I open the Case Contacts tab
    Then I click on new contact button from Case Contacts tab
    And I create a new basic contact to export from Cases Contacts tab
    And I open the Case Contacts tab
    And I click Export button in Case Contacts Directory
    And I click on Detailed Export button in Case Contacts Directory
    And I close popup after export in Case Contacts directory
    Then I click on the Import button from Case Contacts directory
    And I select the case contact CSV file in the file picker
    And I click on the "START DATA IMPORT" button from the Import Case Contacts popup
    And I select first existing person from the Case Contact Import popup
    And I confirm the save Case Contact Import popup
    And I select first existing contact from the Case Contact Import popup
    And I check that an import success notification appears in the Import Case Contact popup
    Then I delete exported file from Case Contact Directory
