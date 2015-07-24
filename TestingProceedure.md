# Introduction #

The following proceedure is a set of steps to follow to test the majority of the functionality of the system.

# Process #

  * Start with a blank install on the phone.
  * Delete all sources and devices from the website. Have it open and ready.
  * Start the program.
    * Initially, the health check area at the bottom will say "no C2DM ID".
    * The accounts and sources button should be disabled.
    * Momentarily, the C2DM ID will come through, and the accounts and sources button will be enabled. If not, restart the app.
    * Now, the health check should say "no accounts selected".
  * Select "Accounts and sources".
    * You will get a first time help message. Dismiss it.
  * Check the box against the relevant google account.
    * A modal dialog will appear when registering.
    * You will be prompted to grant permission to Google app engine.
    * The initial registration will then fail. Tap the checkbox to start it again.
    * A toast message should report success.
    * The checkbox should be checked afterwards.
    * Ensure the device appears on the profile on the website.
  * Tap the account name.
    * Wait whilst the initial (empty) list is fetched from the server.
    * This should complete successfully.
  * Press menu and create a new source.
    * Type in a source name.
    * Wait whilst this is registered with the server.
    * The source should appear in the list on the phone.
    * The source should also be listed on the server if you refresh the page.
  * Edit the source on the phone by tapping it.
    * Change the name slightly.
    * Uncheck both enabled checkboxes.
    * Save the source.
  * You should now be dropped back to the sources list page.
    * The source will have red text underneath it saying that the source is disabled on the server.
    * Check the box in the source list. The red text will stay there.
  * Edit the source again, enable the source on the server, and change the title back.
  * On the source detail page, click "Test".
    * You will get a message that it is contacting the server.
    * Momentarily, you will get a notification and it will speak the test message.
    * In the status bar, an icon will appear.
    * Tap the icon - it should go to the message list for that source.
  * In the message list:
    * The timestamp should be in your local time zone.
    * The message title should be bold (indicating unseen)
  * Tap the message.
    * The message detail screen should appear.
  * Go back.
    * The message title should not be bold anymore.
  * From the website, create a new source. Give it a name that implies it was created on the server.
  * Send a test message quickly from that source.
  * In accounts and sources, list the sources for the account. The new source should appear there.
  * On the website, delete the source.
  * On the phone, go back to the home screen, and then view the list of sources for that account. The list should refresh from the server automatically. The program doesn't refresh immediately; as deleted sources can wait until later.
  * Create a new source on the phone.
    * Confirm that it appears on the server.
  * Delete the new source from the phone.
    * It should delete it from the server immediately.
  * From the website, deregister the device by deleting it.
    * On the phone, the account should be unselected in the accounts and sources screen.
  * Re-register the phone.
  * Go into the sources list. This should refresh from the server the first time.

This completes the core feature testing.