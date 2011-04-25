# Notifry - Google App Engine backend
# 
# Copyright 2011 Daniel Foote
#
# Licensed under the Apache License, Version 2.0 (the 'License');
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an 'AS IS' BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


import datetime
import logging
import urllib
import base64
from model.UserDevice import UserDevice
from model.AC2DMAuthToken import AC2DMAuthToken
from google.appengine.api.urlfetch import fetch
from google.appengine.api import mail

class AC2DM:
	def __init__(self, token):
		self.token = token

	@staticmethod
	def factory():
		token = AC2DMAuthToken.get_latest()
		return AC2DM(token)

	def send_to_google(self, params):
		result = fetch(
			"https://android.apis.google.com/c2dm/send",
			urllib.urlencode(params), # POST body
			"POST", # HTTP method
			{
				'Authorization': 'GoogleLogin auth=' + self.token.token
			}, # Additional headers
			False, # Don't allow a truncated response
			True, # Do follow redirects.
			None, # Default timeout/deadline
			False # Don't validate the SSL cert - there isn't a proper one on there at the moment.
		)

		if result.status_code != 200:
			logging.error('Unable to send message to Google: Status code ' + str(result.status_code) + ' body ' + result.content)

		return result

	def notify_all_source_change(self, source, originating_device_id):
		devices = UserDevice.devices_for(source.owner)
		for device in devices:
			# Skip the device if that was the device that originated the request.
			if device.key().id() == originating_device_id:
				continue
			self.notify_source_change(source, device)

	def notify_source_change(self, source, device):
		params = {}
		params['collapse_key'] = 'source_' + str(source.key().id())
		params['registration_id'] = device.deviceKey
		# Don't wait - let the device know now.
		params['delay_until_idle'] = 0

		params['data.type'] = "sourcechange"
		params['data.id'] = str(source.key().id())
		params['data.device_id'] = str(device.key().id())

		result = self.send_to_google(params)

		if result.status_code == 200:
			# Success!
			return True
		else:
			# Failed to send. Oh well. TODO: handle this?
			return False

	def notify_all_source_delete(self, source, originating_device_id):
		devices = UserDevice.devices_for(source.owner)
		for device in devices:
			# Skip the device if that was the device that originated the request.
			if device.key().id() == originating_device_id:
				continue
			self.notify_source_delete(source, device)

	def notify_source_delete(self, source, device):
		params = {}
		# No need to send any more than one of these in one go.
		params['collapse_key'] = 'refreshall'
		params['registration_id'] = device.deviceKey
		# This request can wait until the device wakes up.
		params['delay_until_idle'] = 1

		params['data.type'] = "refreshall"
		params['data.device_id'] = str(device.key().id())

		result = self.send_to_google(params)

		if result.status_code == 200:
			# Success!
			return True
		else:
			# Failed to send. Oh well. TODO: handle this?
			return False

	def notify_device_delete(self, device):
		params = {}
		# No need to send any more than one of these in one go.
		params['collapse_key'] = 'devicedelete'
		params['registration_id'] = device.deviceKey
		# This request can wait until the device wakes up.
		params['delay_until_idle'] = 1

		params['data.type'] = "devicedelete"
		params['data.device_id'] = str(device.key().id())

		result = self.send_to_google(params)

		if result.status_code == 200:
			# Success!
			return True
		else:
			# Failed to send. Oh well. TODO: handle this?
			return False

	def send_to_all(self, message):
		devices = UserDevice.devices_for(message.source.owner)

		for device in devices:
			self.send(message, device)

		message.deliveredToGoogle = True
		message.lastDeliveryAttempt = datetime.datetime.now()
		message.put()

	def encode(self, input):
		return base64.b64encode(unicode(input).encode('utf-8'))

	def send(self, message, device):
		# Prepare for our request.
		params = {}
		params['collapse_key'] = message.hash()
		params['registration_id'] = device.deviceKey
		params['delay_until_idle'] = 0

		params['data.type'] = "message";
		params['data.server_id'] = message.key().id()
		params['data.source_id'] = message.source.key().id()
		params['data.device_id'] = device.key().id()
		params['data.title'] = self.encode(message.title)
		params['data.message'] = self.encode(message.message)
		if message.url:
			params['data.url'] = self.encode(message.url)
		params['data.timestamp'] = message.timestamp.isoformat()

		result = self.send_to_google(params)

		if result.status_code == 200:
			# Success!
			# The result body is a queue id. Store it.
			message.googleQueueIds.append(str(device.key().id()) + ": " + result.content.strip())
			message.put()
			return True
		else:
			# Failed to send. Log the error message.
			message.put()
			errorMessage = 'Unable to send message ' + message.key().id() + ' to Google: Status code ' + str(result.status_code) + ' body ' + result.content
			logging.error(errorMessage)

			# During BETA testing, send me an email when this fails.
			mail.send_mail(sender="Daniel Foote <freefoote@gmail.com>", to="Daniel Foote <freefoote@gmail.com>", subject="Error sending message", body=errorMessage)
			return False
