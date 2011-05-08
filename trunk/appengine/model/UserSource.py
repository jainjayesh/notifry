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

from google.appengine.ext import db
import datetime
import hashlib
import random
from lib.AC2DM import AC2DM

class UserSource(db.Model):
	owner = db.UserProperty()
	title = db.StringProperty()
	created = db.DateTimeProperty(auto_now_add=True)
	updated = db.DateTimeProperty()
	description = db.StringProperty(multiline=True)
	externalKey = db.StringProperty()
	enabled = db.BooleanProperty()

	def dict(self):
		result = {
			'type' : 'source',
			'title': self.title,
			'created': self.created,
			'updated': self.updated,
			'description': self.description,
			'key': self.externalKey,
			'enabled': self.enabled
		}

		try:
			result['id'] =  self.key().id()
		except db.NotSavedError, ex:
			# Not saved yet, so it has no ID.
			pass

		return result

	def notify(self, originating_device_id):
		# Let the user's devices know that the source has been added or changed.
		ac2dm = AC2DM.factory()
		ac2dm.notify_all_source_change(self, originating_device_id)

	def notify_delete(self, originating_device_id):
		# Let the user's devices know that the source has been deleted. It can
		# resync their list with the server.
		ac2dm = AC2DM.factory()
		ac2dm.notify_all_source_delete(self, originating_device_id)

	@staticmethod
	def generate_key():
		random_key = str(str(datetime.datetime.now()) + str(random.random()) + str(random.random())) + "salt for good measure and a healthy heart"
		digest = hashlib.md5(random_key).hexdigest()
		return digest

	@staticmethod
	def factory(collection):
		raw_key = UserSource.generate_key()
		return UserSource(parent=collection, owner=collection.owner, externalKey=raw_key)