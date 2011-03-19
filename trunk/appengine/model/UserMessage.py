from google.appengine.ext import db
from model.UserSource import UserSource

class UserMessage(db.Model):
	source = db.ReferenceProperty(UserSource)
	timestamp = db.DateTimeProperty()

	title = db.StringProperty()
	message = db.StringProperty(multiline=True)
	url = db.StringProperty()

	deliveredToGoogle = db.BooleanProperty()
	lastDeliveryAttempt = db.DateTimeProperty()
	googleQueueId = db.StringProperty()
	sourceIp = db.StringProperty()

	def dict(self):
		result = {
			'source': self.source,
			'timestamp': self.timestamp,
			'title': self.title,
			'message': self.message,
			'url': self.url,
			'deliveredToGoogle': self.deliveredToGoogle,
			'lastDeliveryAttempt': self.lastDeliveryAttempt,
			'googleQueueId': self.googleQueueId
		}

		try:
			result['id'] =  self.key().id()
		except db.NotSavedError, ex:
			# Not saved yet, so it has no ID.
			pass

		return result

	@staticmethod
	def deleteForSource(source):
		messages = UserMessage.all(keys_only=True)
		messages.filter('source = ', source)
		db.delete(messages)

	@staticmethod
	def deleteOlderThan(date):
		messages = UserMessage.all(keys_only=True)
		messages.filter('timestamp <', date)
		db.delete(messages)