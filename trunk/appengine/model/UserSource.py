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

	def new_object(self):
		self.generate_key()

	def generate_key(self):
		random_key = str(random.random()) + str(datetime.datetime.now()) + str(random.random()) + "salt for good measure and a healthy heart"
		digest = hashlib.md5(random_key).hexdigest()
		self.externalKey = digest

	def dict(self):
		result = {
			'type' : 'source',
			'title': self.title,
			'created': self.created,
			'updated': self.updated,
			'description': self.description,
			'enabled': self.enabled,
			'key': self.externalKey
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
	def find_for_key(key):
		query = UserSource.all()
		query.filter('externalKey =', key)

		if query.count(5) > 0:
			return query[0]
		else:
			return None