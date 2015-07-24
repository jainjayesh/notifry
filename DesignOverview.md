# Design Goals #

Notifry has some very simple goals:

  * Be able to be a drop in replacement for sending server notifications from systems such as Nagios, using push notifications instead of SMS.
    * This means Nagios shouldn't need much information; ideally just a single unique "key" to send the notifications to a given user.
  * Be easy to use with any server monitoring system, not just Nagios.
    * A simple HTTP GET or POST with the correct key and information will get the message delivered.
  * Automatically read out the messages with the Android application, if desired.
  * Send as little information as possible via push. It's not meant to send War and peace, it's just meant to alert the user. So limit the push notification to 1024 bytes.

# Push message #

The push message should be very short. The schema should be very simple too - this is not meant to be a complicated application.

Push notifications themselves will be limited to 1024 bytes and contain just this information:

  * Timestamp (ISO format, always in UTC)
  * Title (a short description of the message) (Required).
  * Source (the user defined source name)
  * URL (optional, can be used to link to info about the message)
  * Message (required).

# Complications #

Push notifications seem to be reasonably reliable. But are they as reliable as SMS? Notifry as a single source of notifications for issues seems like a bad idea. Testing will reveal how well it works.

# Backend #

To provide high availability and reliability on the backend, Google App Engine will be used. This will allow for a lot of redundancy without any special engineering on my part.

# Future enhancements #

  * iPhone client? Or other phone operating system clients?