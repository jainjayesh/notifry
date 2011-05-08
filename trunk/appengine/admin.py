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
from google.appengine.api import users
from lib.Renderer import Renderer
from lib.AC2DM import AC2DM
from model.AC2DMAuthToken import AC2DMAuthToken
from model.AC2DMAuthToken import AC2DMTokenException
from model.UserDevice import UserDevice
from model.UserSource import UserSource
from model.UserMessage import UserMessage
from model.UserDevices import UserDevices
from model.UserSources import UserSources
from model.UserMessages import UserMessages
from model.SourcePointer import SourcePointer
import datetime

urls = (
	'/admin/', 'index',
	'/admin/token/(.*)', 'token',
	'/admin/createtoken/', 'createtoken',
	'/admin/stats/(.*)', 'stats',
	'/admin/migrate', 'migrate'
)

# Create the renderer and the initial context.
renderer = Renderer('templates/')
renderer.addTemplate('title', '')
renderer.addTemplate('user', users.get_current_user())

# Front page of Admin.
class index:
	def GET(self):
		return renderer.render('admin/index.html')

class stats:
	def GET(self, name):
		if name == '':
			# Stats index.
			return renderer.render('admin/stats/index.html')
		if name == 'counters':
			# Counters.
			summary = AC2DM.get_counter_summary()
			summary['buckets'].reverse()
			renderer.addData('counters', summary)
			return renderer.render('admin/stats/counters.html')

# Tokens list.
class token:
	def GET(self, action):
		if action == 'create' or action == 'edit':
			token = self.get_token()
			renderer.addTemplate('action', action)
			
			form = self.get_form()
			form.fill(token.dict())

			renderer.addTemplate('form', form)
			return renderer.render('admin/token/edit.html')
		elif action == 'get':
			# Just get the object.
			token = self.get_token()
			renderer.addData('token', token)
			return renderer.render('admin/token/detail.html')
		else:
			# List.
			tokens = AC2DMAuthToken.all()
			tokens.order('-updated')

			renderer.addDataList('tokens', tokens)
			return renderer.render('admin/token/list.html')

	def POST(self, action):
		token = self.get_token()

		# Get the form and the form data.
		form = self.get_form()
		form.fill(token.dict())

		if not form.validates():
			# Failed to validate. Display the form again.
			renderer.addTemplate('action', action)
			renderer.addTemplate('form', form)
			errors = form.getnotes()
			renderer.addDataList('errors', errors)
			return renderer.render('admin/token/edit.html')
		else:
			# Validated - proceed.
			token.updated = datetime.datetime.now()
			token.token = form.token.get_value()
			token.comment = form.comment.get_value()
			token.put()

			if renderer.get_mode() == 'html':
				# Redirect to the list.
				web.found('/admin/token/')
			else:
				# Send back the source data.
				renderer.addData('token', token)
				return renderer.render('apionly.html')

	def get_token(self):
		# Helper function to get the token object from the URL.
		input = web.input(id=None)
		if input.id:
			# Load token by ID.
			token = AC2DMAuthToken.get_by_id(long(input.id))
			if not token:
				# It does not exist.
				web.notfound()

			return token
		else:
			# New source.
			token = AC2DMAuthToken()
			token.new_object()
			return token

	def get_form(self):
		# Token editor form.
		token_form = web.form.Form(
			web.form.Hidden('id'),
			web.form.Textbox('token', web.form.notnull, description = 'Token'),
			web.form.Textarea('comment', description = 'Comment'),
			web.form.Button('Save')
		)
		return token_form()

# Create token.
class createtoken:
	def GET(self):
		form = self.get_form()

		renderer.addTemplate('form', form)
		return renderer.render('admin/token/login.html')

	def POST(self):
		# Get the form and the form data.
		form = self.get_form()

		if not form.validates():
			# Failed to validate. Display the form again.
			renderer.addTemplate('form', form)
			errors = form.getnotes()
			renderer.addDataList('errors', errors)
			return renderer.render('admin/token/login.html')
		else:
			# Validated.
			# Attempt to get an auth token.
			try:
				token = AC2DMAuthToken.from_username_password(form.username.get_value(), form.password.get_value())
				token.put()

				if renderer.get_mode() == 'html':
					# Redirect to the list.
					web.found('/admin/token/')
				else:
					# Send back the source data.
					renderer.addData('token', token)
					return renderer.render('apionly.html')
			except AC2DMTokenException, e:
				# Failed for some reason!
				renderer.addData('error', str(e))
				renderer.addTemplate('form', form)
				return renderer.render('admin/token/login.html')

	def get_form(self):
		login_form = web.form.Form(
			web.form.Textbox('username', web.form.notnull, description = 'Username'),
			web.form.Textbox('password', web.form.notnull, description = 'Password'),
			web.form.Button('Login')
		)
		return login_form()

class migrate:
	def GET(self):
		# Migrate the old version to the new version. This is heavy,
		# but only a handful of users need this.
		# PHASE 1: Put devices into collections.
		device_collections_user = {}
		for device in UserDevice.all():
			collection_key = device.owner.nickname()
			if not device_collections_user.has_key(collection_key):
				device_collections_user[collection_key] = UserDevices.get_user_device_collection(device.owner)
			device_collections_user[collection_key].add_device(device)

		for key, collection in device_collections_user.iteritems():
			collection.put()

		# PHASE 2: Put messages into collections.
		message_collections_user = {}
		for message in UserMessage.all():
			collection_key = message.owner.nickname()
			if not message_collections_user.has_key(collection_key):
				message_collections_user[collection_key] = UserMessages.get_user_message_collection(message.owner)
			message_collections_user[collection_key].add_message(message)

		for key, collection in message_collections_user.iteritems():
			collection.put()

		# PHASE 3: Put sources into collections. Also create pointers as we go.
		source_collections_user = {}
		for source in UserSource.all():
			collection_key = source.owner.nickname()
			if not source_collections_user.has_key(collection_key):
				source_collections_user[collection_key] = UserSources.get_user_source_collection(source.owner)
			source_collections_user[collection_key].add_source(source)
			SourcePointer.persist(source)

		for key, collection in source_collections_user.iteritems():
			collection.put()

		# And complete.
		return renderer.render('apionly.html')

# Initialise and run the application.
app = web.application(urls, globals())
main = app.cgirun()