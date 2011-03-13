import web
from google.appengine.api import users
from lib.Renderer import Renderer
from model.UserSource import UserSource
from model.UserDevice import UserDevice
from model.UserMessage import UserMessage
import datetime

urls = (
	'/', 'index',
	'/login', 'login',
	'/logout', 'logout',
	'/sources/(.*)', 'sources',
	'/messages/', 'messages',
	'/registration', 'registerdevice',
	'/profile', 'profile',
	'/notifry', 'notifry'
)

# Create the renderer and the initial context.
renderer = Renderer('templates/')
renderer.addTemplate('user', users.get_current_user())

# Helper function to make sure the user is aware that login is required.
def login_required():
	if not users.get_current_user():
		if renderer.get_mode() == 'html':
			# Redirect to a login page, coming back here when done.
			raise web.found(users.create_login_url(web.url()))
		elif renderer.get_mode() == 'json':
			# Return an error in JSON.
			renderer.addData('error', 'Not logged in.')
			return renderer.render('apionly.html')

# Front page.
class index:
	def GET(self):
		# No login required.
		return renderer.render('index.html')

# Login
class login:
	def GET(self):
		user = users.get_current_user()

		if user:
			# Is logged in.
			raise web.found('/profile')
		else:
			# Not logged in - redirect to login.
			raise web.found(users.create_login_url(web.url()))

# Logout
class logout:
	def GET(self):
		raise web.found(users.create_logout_url("/"))

# Profile - list of sources and registered devices.
class profile:
	def GET(self):
		# Must be logged in.
		login_required()

		# List all their sources.
		sources = UserSource.all()
		sources.filter('owner = ', users.get_current_user())
		sources.order('title')

		renderer.addData('sources', sources)

		# List all their devices.
		devices = UserDevice.all()
		devices.filter('owner = ', users.get_current_user())
		devices.order('-updated')

		renderer.addData('devices', devices)

		return renderer.render('profile/index.html')

# Register my device.
class registerdevice:
	def GET(self):
		# For debugging, call POST.
		# This is an easy way to register a device using get params.
		return self.POST()

	def POST(self):
		# You must be logged in.
		login_required()

		# And we need the following variables.
		# The defaults are provided below.
		input = web.input(devicekey = None, devicetype = None, id = None, deviceversion = None)

		# We must have the following keys passed,
		# otherwise this is an invalid request.
		if not input.devicekey and not input.devicetype:
			# Fail with an error.
			renderer.addData('error', 'Missing required parameters "devicekey" and "devicetype".')
			return renderer.render('apionly.html')

		# Check 'devicetype' is 'android' - nothing else is supported right now.
		if input.devicetype != 'android':
			renderer.addData('error', 'Only Android devices are supported at the moment, sorry.')
			return renderer.render('apionly.html')

		# If ID supplied, find and update that ID.
		device = UserDevice()
		if input.id:
			# Load device from ID.
			device = UserDevice.get_by_id(long(input.id))

			if not device:
				# Invalid ID. 404.
				web.notfound()

			# Check that the device belongs to the logged in user.
			if device.owner.user_id() != users.get_current_user().user_id():
				# It's not theirs. 404.
				# TODO: Test this more and better.
				web.notfound()

		# TODO: ensure dates are in UTC.
		device.updated = datetime.datetime.now()
		device.owner = users.get_current_user()
		device.deviceKey = input.devicekey
		device.deviceType = input.devicetype
		device.deviceVersion = input.deviceversion

		device.put()

		renderer.addData('device', device)
		return renderer.render('apionly.html')

# Sources list.
class sources:
	def GET(self, action):
		if action == 'create' or action == 'edit':
			source = self.get_source()
			renderer.addTemplate('action', action)
			
			form = self.get_form()
			form.fill(source.dict())

			renderer.addTemplate('form', form)
			return renderer.render('sources/edit.html')
		elif action == 'get':
			# Just get the object.
			source = self.get_source()
			renderer.addData('source', source)
			return renderer.render('sources/detail.html')
		else:
			# List. Not fully supported - see the profile instead.
			# Although - handy for API's.
			sources = UserSource.all()
			sources.filter('owner = ', users.get_current_user())
			sources.order('title')

			renderer.addDataList('sources', sources)
			return renderer.render('sources/list.html')

	def POST(self, action):
		source = self.get_source()

		# Get the form and the form data.
		form = self.get_form()
		form.fill(source.dict())

		if not form.validates():
			# Failed to validate. Display the form again.
			renderer.addTemplate('action', action)
			renderer.addTemplate('form', form)
			errors = form.getnotes()
			renderer.addDataList('errors', errors)
			return renderer.render('sources/edit.html')
		else:
			# Validated - proceed.
			source.updated = datetime.datetime.now()
			source.title = form.title.get_value()
			source.description = form.description.get_value()
			source.enabled = False
			source.owner = users.get_current_user()
			if form.enabled.get_value():
				source.enabled = True
			source.put()

			if renderer.get_mode() == 'html':
				# Redirect to the source list.
				web.found('/profile')
			else:
				# Send back the source data.
				renderer.addData('source', source)
				return renderer.render('apionly.html')

	def get_source(self):
		# Helper function to get the source object from the URL.
		input = web.input(id=None)
		if input.id:
			# Load source by ID.
			source = UserSource.get_by_id(long(input.id))
			if not source:
				# It does not exist.
				web.notfound()

			# Check that the source belongs to the logged in user.
			if source.owner.user_id() != users.get_current_user().user_id():
				# It's not theirs. 404.
				web.notfound()

			return source
		else:
			# New source.
			source = UserSource()
			source.new_object()
			return source

	def get_form(self):
		# Source editor form.
		source_editor_form = web.form.Form(
			web.form.Hidden('id'),
			web.form.Textbox('title', web.form.notnull, description = 'Title'),
			web.form.Textarea('description', description = 'Description'),
			web.form.Checkbox('enabled', description = 'Enabled'),
			web.form.Button('Save')
		)
		return source_editor_form()

# Notifry someone.
class notifry:
	def GET(self):
		# For debugging, call POST.
		# This is an easy way to send a message using get params.
		return self.POST()

	def POST(self):
		# And we need the following variables.
		# The defaults are provided below.
		input = web.input(source = None, message = None, title = None, url = None)

		# We must have the following keys passed,
		# otherwise this is an invalid request.
		if not input.source or not input.message or not input.title:
			# Fail with an error.
			renderer.addData('error', 'Missing required parameters - need at least source, message, and title.')
			return renderer.render('apionly.html')

		# Find the source matching the source key.
		source = UserSource.find_for_key(input.source)

		if not source:
			# No such source.
			renderer.addData('error', 'No source matches the key ' + str(input.source))
			return renderer.render('apionly.html')

		# Create the message object.
		message = UserMessage()
		message.source = source
		message.message = input.message
		message.title = input.title
		if input.url:
			message.url = input.url
		message.timestamp = datetime.datetime.now()
		message.deliveredToGoogle = False
		message.lastDeliveryAttempt = None
		message.sourceIp = web.ctx.ip
		message.put()

		# TODO: Check it's not all bigger than 1024 bytes all up.
		# And handle the edge cases associated with that.

		# Now that it's saved, send it to Google.

		renderer.addData('message', message)
		return renderer.render('apionly.html')

# Messages - list of messages in the system.
class messages:
	def GET(self):
		# Must be logged in.
		login_required()

		# List all their sources.
		sources = UserSource.all()
		sources.filter('owner = ', users.get_current_user())
		sources.order('title')

		renderer.addData('sources', sources)

		# List messages, optionally filtered by the source.
		source = self.get_source()
		messages = UserMessage.all()
		if source:
			messages.filter('source =', source)
		messages.order('-timestamp')

		renderer.addData('filtersource', source)
		renderer.addData('messages', messages)

		return renderer.render('messages/index.html')

	def get_source(self):
		# Helper function to get the source object from the URL.
		input = web.input(sid=None)
		if input.sid:
			# Load source by ID.
			source = UserSource.get_by_id(long(input.sid))
			if not source:
				# It does not exist.
				web.notfound()

			# Check that the source belongs to the logged in user.
			if source.owner.user_id() != users.get_current_user().user_id():
				# It's not theirs. 404.
				web.notfound()

			return source
		else:
			# No source selected.
			return None

# Initialise and run the application.
app = web.application(urls, globals())
main = app.cgirun()