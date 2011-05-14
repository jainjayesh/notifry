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
from model.SourcePointer import SourcePointer
import datetime
import hashlib

SIZE_LIMIT = 512

class ExceptionUserMessage(Exception):
	pass

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
		input = unicode(self.title) + unicode(self.message) + unicode(self.url)
		digest = hashlib.md5(input.encode('ascii', 'xmlcharrefreplace')).hexdigest()
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
	def create_test(source, ip, collection):
		message = UserMessage(parent=collection)
		message.owner = source.owner
		message.source = source
		message.message = "This is a test message."
		message.title = "Test Message"
		message.timestamp = datetime.datetime.now()
		message.deliveredToGoogle = False
		message.lastDeliveryAttempt = None
		message.sourceIp = ip
		message.wasTruncated = False

		return message

	@staticmethod
	def from_web(source_key, input, ip, UserMessages):
		# Find the source matching the source key.
		source = SourcePointer.get_source(source_key)

		if not source:
			raise ExceptionUserMessage(source_key + ': No source matches this key')

		# If the source is server disabled, let the calling sysadmin know.
		if not source.enabled:
			raise ExceptionUserMessage(source_key + ': User has this source disabled on the server')

		# Create the message object.
		message_collection = UserMessages.get_user_message_collection(source.owner)
		message = UserMessage(parent=message_collection)
		message.owner = source.owner
		message.source = source
		if input.message:
			message.message = input.message
		else:
			message.message = ''
		message.title = input.title
		if input.url:
			message.url = input.url
		message.timestamp = datetime.datetime.now()
		message.deliveredToGoogle = False
		message.lastDeliveryAttempt = None
		message.sourceIp = ip

		if not message.checksize():
			# Too big! We've done our best, but...
			raise ExceptionUserMessage(source_key + ': Your message is too big. The title and URL were too long and the message could not be trimmed to fit. Maximum size is nearly 500 bytes')

		return message
