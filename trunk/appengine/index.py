import web
from google.appengine.api import users
from lib.Renderer import Renderer
from model.UserSource import UserSource
from model.UserDevice import UserDevice
from model.UserMessage import UserMessage
import datetime
from lib.AC2DM import AC2DM

urls = (
	'/', 'index',
	'/login', 'login',
	'/logout', 'logout',
	'/sources/(.*)', 'sources',
	'/messages/', 'messages',
	'/devices/(.*)', 'devices',
	'/profile', 'profile',
	'/notifry', 'notifry',
	'/page/(.*)', 'page',
)

# Create the renderer and the initial context.
renderer = Renderer('templates/')
renderer.addTemplate('user', users.get_current_user())
renderer.addTemplate('title', '')
renderer.addTemplate('dateformat', '%A, %d %B %Y %H:%M UTC')

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

# Handle devices.
class devices:
	def GET(self, action):
		# You must be logged in.
		login_required()

		if action == "register":
			# For debugging, call POST.
			# This is an easy way to register a device using get params.
			return self.POST(action)
		elif action == "delete":
			# Show confirmation screen.
			device = self.get_device()
			renderer.addData('device', device)
			return renderer.render('device/delete.html')
		else:
			# List devices.
			renderer.addDataList('devices', UserDevice.devices_for(users.get_current_user()))
			return renderer.render('apionly.html')

	def POST(self, action):
		# You must be logged in.
		login_required()

		if action == 'delete':
			device = self.get_device()

			# Let the device know it's been deleted.
			ac2dm = AC2DM.factory()
			ac2dm.notify_device_delete(device)

			# Now delete it.
			device.delete()
			renderer.addData('success', True)
			return renderer.render('device/deletecomplete.html')
		elif action == 'deregister':
			device = self.get_device()
			device.delete()
			renderer.addData('success', True)
			return renderer.render('apionly.html')
		elif action == 'register':
			# And we need the following variables.
			# The defaults are provided below.
			input = web.input(devicekey = None, devicetype = None, deviceversion = None, nickname = None)

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
			device = self.get_device()

			# Is it a new device? Double check that the key isn't already
			# attached to another record (with the same owner!)
			if not device.dict().has_key('id'):
				# Attempt to find another device with the same key.
				same_key = UserDevice.device_by_key(users.get_current_user(), input.devicekey, input.devicetype)

				# If found, use that instead.
				if same_key:
					device = same_key

			device.updated = datetime.datetime.now()
			device.owner = users.get_current_user()
			device.deviceKey = input.devicekey
			device.deviceType = input.devicetype
			device.deviceVersion = input.deviceversion
			device.deviceNickname = input.nickname

			device.put()
			renderer.addData('device', device)
			return renderer.render('apionly.html')

	def get_device(self):
		# If ID supplied, find and update that ID.
		input = web.input(id = None)

		device = UserDevice()
		if input.id and long(input.id) > 0:
			# Load device from ID.
			device = UserDevice.get_by_id(long(input.id))

			if not device:
				# Invalid ID. 404.
				raise web.notfound()

			# Check that the device belongs to the logged in user.
			if device.owner.user_id() != users.get_current_user().user_id():
				# It's not theirs. 404.
				# TODO: Test this more and better.
				raise web.notfound()

		return device

# Sources list.
class sources:
	def GET(self, action):
		if action == 'create' or action == 'edit':
			source = self.get_source()
			renderer.addTemplate('action', self.get_pretty_action(action))
			
			form = self.get_form()
			form.fill(source.dict())

			renderer.addTemplate('form', form)
			return renderer.render('sources/edit.html')
		elif action == 'get':
			# Just get the object.
			source = self.get_source()
			renderer.addData('source', source)
			return renderer.render('sources/detail.html')
		elif action == 'delete':
			# Show deletion information page.
			source = self.get_source()
			renderer.addData('source', source)
			return renderer.render('sources/delete.html')
		elif action == 'test':
			# Use the same handler as the POST action.
			return self.POST(action)
		else:
			# List. Not fully supported - see the profile instead.
			# Although - handy for API's.
			sources = UserSource.all()
			sources.filter('owner = ', users.get_current_user())
			sources.order('title')

			renderer.addDataList('sources', sources)
			return renderer.render('sources/list.html')

	def POST(self, action):
		if action == 'list':
			sources = UserSource.all()
			sources.filter('owner = ', users.get_current_user())
			sources.order('title')

			renderer.addDataList('sources', sources)
			return renderer.render('sources/list.html')
		elif action == 'get':
			source = self.get_source()
			renderer.addData('source', source)
			return renderer.render('sources/detail.html')
		elif action == 'test':
			# Send a test message to the source.
			source = self.get_source()

			message = UserMessage.createTest(source, web.ctx.ip)

			sender = AC2DM.factory()
			sender.send_to_all(message)

			# And we're done.
			return renderer.render('sources/test.html')
		elif action == 'delete':
			source = self.get_source()
			UserMessage.deleteForSource(source)

			# Notify devices that something changed.
			# Also, if given a device, exclude that device from
			# the notification.
			input = web.input(device = None)
			source.notify_delete(input.device)

			source.delete()
			renderer.addData('success', True)
			return renderer.render('sources/deletecomplete.html')
		else:
			source = self.get_source()

			# Get the form and the form data.
			form = self.get_form()
			form.fill(source.dict())

			if not form.validates():
				# Failed to validate. Display the form again.
				renderer.addTemplate('action', self.get_pretty_action(action))
				renderer.addTemplate('form', form)
				errors = form.getnotes()
				renderer.addDataList('errors', errors)
				return renderer.render('sources/edit.html')
			else:
				# Validated - proceed.
				source.updated = datetime.datetime.now()
				source.title = form.title.get_value()
				#source.description = form.description.get_value()
				source.enabled = False
				source.owner = users.get_current_user()
				if form.enabled.get_value():
					source.enabled = True
				source.put()

				# Notify devices that something changed.
				# Also, if given a device, exclude that device from
				# the notification.
				input = web.input(device = None)
				source.notify(input.device)

				if renderer.get_mode() == 'html':
					# Redirect to the source list.
					raise web.found('/profile')
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
				raise web.notfound()

			# Check that the source belongs to the logged in user.
			if source.owner.user_id() != users.get_current_user().user_id():
				# It's not theirs. 404.
				raise web.notfound()

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
			web.form.Textbox('title', web.form.notnull, description = 'Title:'),
			#web.form.Textarea('description', description = 'Description:'),
			web.form.Checkbox('enabled', description = 'Enabled:'),
			web.form.Button('Save')
		)
		form = source_editor_form()
		return form

	def get_pretty_action(self, action):
		return action[0].upper() + action[1:]

# Notifry someone.
class notifry:
	def GET(self):
		# GET does the same thing as POST. Allows for easy testing.
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
			return renderer.render('messages/send.html')

		# Find the source matching the source key.
		source = UserSource.find_for_key(input.source)

		if not source:
			# No such source.
			renderer.addData('error', 'No source matches the key ' + str(input.source))
			return renderer.render('messages/send.html')

		# If the source is server disabled, let the calling sysadmin know.
		# TODO: Is this an information leak?
		if not source.enabled:
			renderer.addData('error', 'User has this source disabled on the server.')
			return renderer.render('messages/send.html')

		# Create the message object.
		message = UserMessage()
		message.owner = source.owner
		message.source = source
		message.message = input.message
		message.title = input.title
		if input.url:
			message.url = input.url
		message.timestamp = datetime.datetime.now()
		message.deliveredToGoogle = False
		message.lastDeliveryAttempt = None
		message.sourceIp = web.ctx.ip

		if not message.checksize():
			# Too big! We've done our best, but...
			renderer.addData('error', 'Your message is too big. The title and URL were too long and the message could not be trimmed to fit. Maximum size is nearly 1024 bytes.')
			return renderer.render('messages/send.html')

		message.put()

		# Now that it's saved, send it to Google.
		sender = AC2DM.factory()
		sender.send_to_all(message)

		# Don't return the message - that would leak client information.
		renderer.addData('success', True)
		renderer.addData('size', message.getsize())
		renderer.addData('truncated', message.wasTruncated)
		renderer.addTemplate('error', None)
		return renderer.render('messages/send.html')

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
		messages.filter('owner = ', users.get_current_user())
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
				raise web.notfound()

			# Check that the source belongs to the logged in user.
			if source.owner.user_id() != users.get_current_user().user_id():
				# It's not theirs. 404.
				raise web.notfound()

			return source
		else:
			# No source selected.
			return None

# Static pages.
class page:
	def GET(self, page):
		valid_pages = {
			'faq': 'pages/faq.html',
			'api': 'pages/api.html',
			'privacy': 'pages/privacy.html',
			'sla': 'pages/sla.html',
			'gettingstarted': 'pages/gettingstarted.html',
			'about': 'pages/about.html'
		}

		if valid_pages.has_key(page):
			return renderer.render(valid_pages[page])
		else:
			raise web.notfound()

# Not found handler.
def not_found_handler():
	renderer.addData('error', '404 Not found')
	return web.notfound(renderer.render('404.html'))

def server_error_handler():
	renderer.addData('error', '500 Internal Server Error')
	return web.internalerror(renderer.render('500.html'))

# Initialise and run the application.
app = web.application(urls, globals())
app.notfound = not_found_handler
#app.internalerror = server_error_handler()
main = app.cgirun()
