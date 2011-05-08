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
from model.UserDevices import UserDevices
from model.AC2DMAuthToken import AC2DMAuthToken
from google.appengine.api.urlfetch import fetch
from google.appengine.api import mail
from model.GeneralCounterShard import increment
from model.GeneralCounterShard import get_count

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
		devices = UserDevices.get_user_devices(source.owner)
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
		params['data.id'] = source.key().id()
		params['data.device_id'] = str(device.key().id())

		result = self.send_to_google(params)
		self.increment_counter('source_change')

		if result.status_code == 200:
			# Success!
			return True
		else:
			# Failed to send. Oh well. TODO: handle this?
			return False

	def notify_all_source_delete(self, source, originating_device_id):
		devices = UserDevices.get_user_devices(source.owner)
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
		self.increment_counter('source_delete')

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
		self.increment_counter('device_delete')

		if result.status_code == 200:
			# Success!
			return True
		else:
			# Failed to send. Oh well. TODO: handle this?
			return False

	def send_to_all(self, message):
		devices = UserDevices.get_user_devices(message.owner)

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
		params['data.source_id'] = source.key().id()
		params['data.device_id'] = device.key().id()
		params['data.title'] = self.encode(message.title)
		params['data.message'] = self.encode(message.message)
		if message.url:
			params['data.url'] = self.encode(message.url)
		params['data.timestamp'] = message.timestamp.isoformat()

		result = self.send_to_google(params)
		self.increment_counter('message')

		if result.status_code == 200:
			# Success!
			# The result body is a queue id. Store it.
			message.googleQueueIds.append(str(device.key().id()) + ": " + result.content.strip())
			message.put()
			return True
		else:
			# Failed to send. Log the error message.
			message.put()
			errorMessage = 'Unable to send message ' + str(message.key().id()) + ' to Google: Status code ' + str(result.status_code) + ' body ' + result.content
			logging.error(errorMessage)

			# Send an email when it fails. This should not contain any private data.
			mail.send_mail(sender="Notifry <notifry@gmail.com>", to="Notifry <notifry@gmail.com>", subject="Error sending message", body=errorMessage)
			return False

	def increment_counter(self, name):
		# Get the date.
		date_bucket = datetime.datetime.now().strftime("%Y-%m-%d_%H")
		increment("%s_%s" % (date_bucket, name))

	@staticmethod
	def get_counter_summary():
		# Buckets is the time windows.
		buckets = []
		# And all the message types.
		types = ['message', 'device_delete', 'source_delete', 'source_change']
		# Fill the time windows with hourly buckets for the last 24 hours.
		to_time = datetime.datetime.now()
		from_time = to_time - datetime.timedelta(1)
		hours_difference_delta = datetime.timedelta(0, 0)
		while from_time + hours_difference_delta <= to_time:
			temp_time = from_time + hours_difference_delta
			buckets.append(temp_time.strftime("%Y-%m-%d_%H"))
			hours_difference_delta += datetime.timedelta(0, 3600)
		hour_results = {}
		grand_totals = {}
		# And for each time bucket...
		for bucket in buckets:
			bucket_result = {}
			bucket_total = 0
			# And then for each type...
			for type in types:
				raw_count = int(get_count("%s_%s" % (bucket, type)))
				bucket_result[type] = raw_count
				bucket_total += raw_count
				if not grand_totals.has_key(type):
					grand_totals[type] = 0
				grand_totals[type] += raw_count
			bucket_result['total'] = bucket_total
			if not grand_totals.has_key('total'):
				grand_totals['total'] = 0
			grand_totals['total'] += bucket_total
			hour_results[bucket] = bucket_result

		# Assemble the result data.
		types.append('total')
		final_result = {}
		final_result['data'] = hour_results
		final_result['types'] = types
		final_result['buckets'] = buckets
		final_result['totals'] = grand_totals

		return final_result