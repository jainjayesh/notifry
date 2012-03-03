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

import datetime
from model.UserMessages import UserMessages
from google.appengine.ext import db
from google.appengine.ext.db import BadRequestError

# The deferred function to actually delete messages per collection.
def delete_messages_for_collection(collection_key):
	# Load the collection.
	older_than = datetime.datetime.now() - datetime.timedelta(1)
	collection = UserMessages.get(collection_key)
	def transaction(check_collection):
		# For each message, check the timestamp.
		for message in check_collection.get_messages():
			if message:
				# And delete if older than the given timestamp.
				if message.timestamp < older_than:
					check_collection.remove_message(message)
					message.delete()
			check_collection.put()
	try:
		db.run_in_transaction(transaction, collection)
	except BadRequestError, ex:
		# Typically this is a transaction that has expired. Return normally
		# to prevent retrying this task.
		return
