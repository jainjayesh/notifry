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

	@staticmethod
	def devices_for(owner, type = 'android'):
		devices = UserDevice.all()
		devices.filter('owner = ', owner)
		devices.order("-updated")

		return devices
