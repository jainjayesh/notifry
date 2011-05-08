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
from model.UserSource import UserSource
from model.UserSources import UserSources

class SourcePointer(db.Model):
	owner = db.UserProperty()
	sourceId = db.IntegerProperty()
	externalKey = db.StringProperty()

	def dict(self):
		result = {
			'type' : 'pointer',
			'owner': self.owner,
			'sourceId': self.sourceId,
			'externalKey': self.externalKey
		}

		try:
			result['key'] =  self.key().name()
		except db.NotSavedError, ex:
			# Not saved yet, so it has no name.
			pass

		return result

	@staticmethod
	def persist(source):
		return SourcePointer.get_or_insert(source.externalKey, owner = source.owner, sourceId = source.key().id(), externalKey = source.externalKey)

	@staticmethod
	def remove(source):
		pointer = SourcePointer.get_pointer(source.externalKey)
		if pointer:
			pointer.delete()

	@staticmethod
	def get_pointer(key):
		return SourcePointer.get_by_key_name(key)

	@staticmethod
	def get_source(key):
		pointer = SourcePointer.get_pointer(key)
		if pointer:
			source_collection = UserSources.get_user_source_collection(pointer.owner)
			source = UserSource.get_by_id(pointer.sourceId, source_collection)
			return source
		else:
			return None