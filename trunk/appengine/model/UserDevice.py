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

import hashlib
from google.appengine.ext import db

class UserDevice(db.Model):
	owner = db.UserProperty()
	deviceKey = db.StringProperty()
	created = db.DateTimeProperty(auto_now_add=True)
	updated = db.DateTimeProperty()
	deviceType = db.StringProperty()
	deviceVersion = db.StringProperty()
	deviceNickname = db.StringProperty()

	def hash(self):
		digest = hashlib.md5(self.deviceKey).hexdigest()
		return digest[0:8]

	def dict(self):
		result = {
			'type' : 'device',
			'created': self.created,
			'updated': self.updated,
			'deviceType': self.deviceType,
			'deviceVersion': self.deviceVersion,
			'deviceNickname': self.deviceNickname
		}

		try:
			result['id'] =  self.key().id()
		except db.NotSavedError, ex:
			# Not saved yet, so it has no ID.
			pass

		return result
