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
from model.UserDevice import UserDevice

class UserDevices(db.Model):
	devices = db.ListProperty(int)
	owner = db.UserProperty()

	def dict(self):
		result = {
			'type' : 'devices',
			'owner': self.owner,
			'devices': self.get_devices()
		}

		try:
			result['key'] =  self.key().name()
		except db.NotSavedError, ex:
			# Not saved yet, so it has no ID.
			pass

		return result

	def get_devices(self):
		return UserDevice.get_by_id(self.devices, self)

	def add_device(self, device):
		id = device.key().id()
		if self.devices:
			if not id in self.devices:
				self.devices.append(id)
		else:
			self.devices = []
			self.devices.append(id)

	def remove_device(self, device):
		if self.devices:
			try:
				self.devices.remove(device.key().id())
			except ValueError, ex:
				# We don't have that device in the list.
				pass

	@staticmethod
	def key_for(owner):
		return "devices:%s" % owner.nickname()

	@staticmethod
	def get_user_device_collection(owner):
		return UserDevices.get_or_insert(UserDevices.key_for(owner), owner = owner)

	@staticmethod
	def get_user_device_collection_static(owner):
		return UserDevices.get_by_key_name(UserDevices.key_for(owner))

	@staticmethod
	def get_user_devices(owner):
		collection = UserDevices.get_user_device_collection(owner)
		return collection.get_devices()