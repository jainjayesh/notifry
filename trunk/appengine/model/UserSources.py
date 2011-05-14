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

class UserSources(db.Model):
	sources = db.ListProperty(int)
	owner = db.UserProperty()

	def dict(self):
		result = {
			'type' : 'sources',
			'owner': self.owner,
			'sources': self.get_sources()
		}

		try:
			result['key'] =  self.key().name()
		except db.NotSavedError, ex:
			# Not saved yet, so it has no ID.
			pass

		return result

	def get_sources(self):
		return UserSource.get_by_id(self.sources, self)

	def add_source(self, source):
		id = source.key().id()
		if self.sources:
			if not id in self.sources:
				self.sources.append(id)
		else:
			self.sources = []
			self.sources.append(id)

	def remove_source(self, source):
		if self.sources:
			try:
				self.sources.remove(source.key().id())
			except ValueError, ex:
				# We don't have that source in the list.
				pass

	@staticmethod
	def key_for(owner):
		return "sources:%s" % owner.nickname()

	@staticmethod
	def get_user_source_collection(owner):
		return UserSources.get_or_insert(UserSources.key_for(owner), owner=owner)

	@staticmethod
	def get_user_source_collection_static(owner):
		return UserSources.get_by_key_name(UserSources.key_for(owner))

	@staticmethod
	def get_user_sources(owner):
		collection = UserSources.get_user_source_collection(owner)
		return collection.get_sources()