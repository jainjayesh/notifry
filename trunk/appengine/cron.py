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

import web
from lib.Renderer import Renderer
from model.UserMessages import UserMessages
from model.UserMessage import UserMessage
from google.appengine.ext import deferred
from google.appengine.ext import db
from lib.DeleteHelper import delete_messages_for_collection
import datetime

urls = (
	'/cron/deletemessages', 'deletemessages'
)

# Create the renderer and the initial context.
renderer = Renderer('templates/')

# Cron script to delete messages.
class deletemessages:
	def GET(self):
		# Older than 1 day.
		older_than = datetime.datetime.now() - datetime.timedelta(1)
		# Find all messages.
		messages = UserMessage.all(keys_only=True)
		messages.filter("timestamp < ", older_than)
		results = messages.fetch(200)
		db.delete(results)

		renderer.addData('number', len(results))
		return renderer.render('cron/deletemessages.html')

# Initialise and run the application.
app = web.application(urls, globals())
main = app.cgirun()
