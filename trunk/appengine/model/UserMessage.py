from google.appengine.ext import db
from model.UserSource import UserSource
import datetime

SIZE_LIMIT = 512

class UserMessage(db.Model):
	owner = db.UserProperty()
	source = db.ReferenceProperty(UserSource)
	timestamp = db.DateTimeProperty()

	title = db.StringProperty()
	message = db.TextProperty()
	url = db.StringProperty()
	wasTruncated = db.BooleanProperty()

	deliveredToGoogle = db.BooleanProperty()
	lastDeliveryAttempt = db.DateTimeProperty()
	googleQueueIds = db.StringListProperty()
	sourceIp = db.StringProperty()

	def dict(self):
		result = {
			'source': self.source,
			'timestamp': self.timestamp,
			'title': self.title,
			'message': self.message,
			'url': self.url,
			'wasTruncated': self.wasTruncated,
			'deliveredToGoogle': self.deliveredToGoogle,
			'lastDeliveryAttempt': self.lastDeliveryAttempt,
			'googleQueueIds': self.googleQueueIds
		}

		try:
			result['id'] =  self.key().id()
		except db.NotSavedError, ex:
			# Not saved yet, so it has no ID.
			pass

		return result

	def hash(self):
		digest = hashlib.md5(self.title + self.message + str(self.url)).hexdigest()
		return digest[0:8]

	def getsize(self):
		size = len(self.message)
		size += len(self.title)
		if self.url:
			size += len(self.url)
		size += len(datetime.datetime.now().isoformat())
		size += 12 # The source ID that we pass - assume it's never bigger than 12 chars.

		return size

	def checksize(self):
		# Calculate the size of the message.
		size = self.getsize()

		if size > SIZE_LIMIT:
			# The message is too big.
			# See if we can trim the message to cut it down.
			# We don't cut the title or the URL - especially the URL, as that
			# could break it.
			need_to_trim = size - SIZE_LIMIT
			if len(self.message) < need_to_trim:
				# No possible way to trim it.
				return False
			else:
				# Ok, munge the message.
				# TODO: Make configureable?
				self.message = self.message[0:need_to_trim]
				self.wasTruncated = True

		self.wasTruncated = False
		return True

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

	@staticmethod
	def createTest(source, ip):
		message = UserMessage()
		message.owner = source.owner
		message.source = source
		message.message = "This is a test message."
		message.title = "Test Message"
		message.timestamp = datetime.datetime.now()
		message.deliveredToGoogle = False
		message.lastDeliveryAttempt = None
		message.sourceIp = ip
		message.wasTruncated = False
		message.put()

		return message
