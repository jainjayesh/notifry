from google.appengine.ext import db

class UserDevice(db.Model):
	owner = db.UserProperty()
	deviceKey = db.StringProperty()
	created = db.DateTimeProperty(auto_now_add=True)
	updated = db.DateTimeProperty()
	deviceType = db.StringProperty()
	deviceVersion = db.StringProperty()

	def dict(self):
		result = {
			'type' : 'device',
			'created': self.created,
			'updated': self.updated,
			'deviceType': self.deviceType,
			'deviceVersion': self.deviceVersion
		}

		try:
			result['id'] =  self.key().id()
		except db.NotSavedError, ex:
			# Not saved yet, so it has no ID.
			pass

		return result