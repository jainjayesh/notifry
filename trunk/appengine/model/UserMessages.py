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
from model.UserMessage import UserMessage

class UserMessages(db.Model):
	messages = db.ListProperty(int)
	owner = db.UserProperty()

	def dict(self):
		result = {
			'type' : 'messages',
			'owner': self.owner,
			'messages': self.get_messages()
		}

		try:
			result['key'] =  self.key().name()
		except db.NotSavedError, ex:
			# Not saved yet, so it has no ID.
			pass

		return result

	def get_messages(self):
		return UserMessage.get_by_id(self.messages, self)

	def get_messages_for_source(self, source):
		final_messages = []
		for message in self.get_messages():
			if message.source.externalKey == source.externalKey:
				final_messages.append(message)
		return final_messages

	def add_message(self, message):
		id = message.key().id()
		if self.messages:
			if not id in self.messages:
				self.messages.append(id)
		else:
			self.messages = []
			self.messages.append(id)

	def remove_message(self, message):
		if self.messages:
			try:
				self.messages.remove(message.key().id())
			except ValueError, ex:
				# We don't have that device in the list.
				pass

	def delete_for_source(self, source):
		messages = self.get_messages_for_source(source)
		db.delete(messages)
		for message in messages:
			self.remove_message(message)

	@staticmethod
	def key_for(owner):
		return "messages:%s" % owner.nickname()

	@staticmethod
	def get_user_message_collection(owner):
		return UserMessages.get_or_insert(UserMessages.key_for(owner), owner = owner)

	@staticmethod
	def get_user_messages(owner):
		collection = UserMessages.get_user_message_collection(owner)
		return collection.get_messages()

	@staticmethod
	def get_user_messages_for_source(source):
		collection = UserMessages.get_user_message_collection(source.owner)
		return collection.get_messages_for_source(source)